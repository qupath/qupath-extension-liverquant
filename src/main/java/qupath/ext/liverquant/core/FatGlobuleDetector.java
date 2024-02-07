package qupath.ext.liverquant.core;

import org.bytedeco.javacpp.indexer.Index;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.Moments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.experimental.pixels.OpenCVProcessor;
import qupath.lib.experimental.pixels.OutputHandler;
import qupath.lib.experimental.pixels.Parameters;
import qupath.lib.experimental.pixels.PixelProcessor;
import qupath.lib.geom.Point2;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.TaskRunnerFX;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.plugins.CommandLineTaskRunner;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.RoiTools;
import qupath.opencv.tools.OpenCVTools;
import qupath.opencv.tools.ProcessingCV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;


/**
 * Enable the detection of fat globules within image tiles based on
 * <a href="https://github.com/mfarzi/liverquant">liverquant</a>.
 */
public class FatGlobuleDetector {

    private static final Logger logger = LoggerFactory.getLogger(FatGlobuleDetector.class);
    private enum GlobuleClassification {
        ISOLATED_GLOBULE,
        OVERLAPPING_GLOBULE
    }

    /**
     * Run the detection algorithm. This will not return anything but
     * display progress on a window or on the logs.
     *
     * @param fatGlobulesDetectorParameters  the parameters to use for the detection
     */
    public static void run(FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        PixelProcessor<Mat, Mat, Mat> processor = OpenCVProcessor.builder(params -> {
                    Mat mat = params.getImage().clone();

                    segmentByColor(
                            mat,
                            fatGlobulesDetectorParameters.getLowerBound(),
                            fatGlobulesDetectorParameters.getUpperBound()
                    );
                    fillHoles(mat);

                    return mat;
                })
                .downsample(
                        fatGlobulesDetectorParameters.getPixelSize() <= 0 ? 1 :
                                fatGlobulesDetectorParameters.getPixelSize() / fatGlobulesDetectorParameters.getImageData().getServer().getPixelCalibration().getAveragedPixelSizeMicrons()
                )
                .tile(fatGlobulesDetectorParameters.getTileWidth(), fatGlobulesDetectorParameters.getTileHeight())
                .padding(fatGlobulesDetectorParameters.getPadding())
                .mergeSharedBoundaries(fatGlobulesDetectorParameters.getBoundaryThreshold())
                .outputHandler(OutputHandler.createObjectOutputHandler((Parameters<Mat, Mat> parameters, Mat output) -> {
                    Map<GlobuleClassification, List<Mat>> classifications = findGlobules(
                            output,
                            fatGlobulesDetectorParameters
                    );

                    List<Mat> contoursOfSeparatedOverlappingGlobules = separateOverlappingGlobules(
                            classifications.get(GlobuleClassification.OVERLAPPING_GLOBULE),
                            output.rows(),
                            output.cols()
                    );
                    for (Mat overlappingGlobule : classifications.get(GlobuleClassification.OVERLAPPING_GLOBULE)) {
                        overlappingGlobule.close();
                    }

                    List<Mat> otherGlobules = getIsolatedGlobules(contoursOfSeparatedOverlappingGlobules, fatGlobulesDetectorParameters);

                    List<Mat> globules = new ArrayList<>();
                    globules.addAll(classifications.get(GlobuleClassification.ISOLATED_GLOBULE));
                    globules.addAll(otherGlobules);

                    List<PathObject> pathObjects = createPathObjects(globules, fatGlobulesDetectorParameters.getObjectToCreate(), parameters.getRegionRequest());

                    for (Mat globule : globules) {
                        globule.close();
                    }

                    return pathObjects;
                }))
                .build();

        Executors.newSingleThreadExecutor().execute(() -> {
            processor.processObjects(
                switch (fatGlobulesDetectorParameters.getProgressDisplay()) {
                    case WINDOW -> new TaskRunnerFX(QuPathGUI.getInstance());
                    case LOG -> new CommandLineTaskRunner();
                },
                fatGlobulesDetectorParameters.getImageData(),
                fatGlobulesDetectorParameters.getAnnotations()
            );

            fatGlobulesDetectorParameters.getOnFinished().run();
        });
    }

    /**
     * Segment an RGB image based on the provided arrays in the HSV-space.
     * No new image will be created; instead, the input image will be modified.
     *
     * @param mat  the image to segment with the RGB format. It will be converted to a
     *             {@link opencv_core#CV_8U} image and will contain the result of the segmentation
     * @param lowerBound  inclusive lower bound array in HSV-space for color segmentation
     * @param upperBound  inclusive upper bound array in HSV-space for color segmentation
     */
    private static void segmentByColor(Mat mat, FatGlobulesDetectorParameters.HsvArray lowerBound, FatGlobulesDetectorParameters.HsvArray upperBound) {
        try (
                Scalar lowerBoundScalar = new Scalar(
                        lowerBound.hue(),
                        lowerBound.saturation(),
                        lowerBound.value(),
                        255
                );
                Mat lowerBoundMat = new Mat(lowerBoundScalar);
                Scalar upperBoundScalar = new Scalar(
                        upperBound.hue(),
                        upperBound.saturation(),
                        upperBound.value(),
                        255
                );
                Mat upperBoundMat = new Mat(upperBoundScalar)
        ) {
            mat.convertTo(mat, opencv_core.CV_8U);
            opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_RGB2HSV);
            opencv_core.inRange(mat, lowerBoundMat, upperBoundMat, mat);
        }
    }

    /**
     * Fill all holes of a mask with white pixels. The provided mask will be modified.
     *
     * @param mask  the mask containing the holes to fill. It must have the {@link opencv_core#CV_8U} format
     */
    private static void fillHoles(Mat mask) {
        try (
                MatVector contours = new MatVector();
                Mat hierarchy = new Mat()
        ) {
            opencv_imgproc.findContours(mask, contours, hierarchy, opencv_imgproc.RETR_CCOMP, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            try (IntRawIndexer indexer = hierarchy.createIndexer()) {
                for (int i=0; i<contours.size(); ++i) {
                    if (indexer.get(0, i, 3) > -1) {
                        Mat[] contoursArray = new Mat[] {contours.get(i)};

                        try (
                                MatVector contoursToDraw = new MatVector(contoursArray);
                                Scalar color = new Scalar(255);
                                Mat hierarchyToDraw = new Mat();
                                Point offset = new Point(0, 0)
                        ) {
                            opencv_imgproc.drawContours(
                                    mask,
                                    contoursToDraw,
                                    0,
                                    color,
                                    -1,
                                    opencv_imgproc.LINE_8,
                                    hierarchyToDraw,
                                    Integer.MAX_VALUE,
                                    offset
                            );
                        }
                    }

                    contours.get(i).close();
                }
            }
        }
    }

    /**
     * Find the contours of the provided mask and classify them as isolated or overlapping globules.
     *
     * @param mask  the mask whose globules should be found
     * @param fatGlobulesDetectorParameters  the parameters to use during the classification
     * @return a map containing the contours of the provided mask classified as isolated or overlapping globules
     */
    private static Map<GlobuleClassification, List<Mat>> findGlobules(Mat mask, FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        return filterGlobules(
                findContours(mask),
                fatGlobulesDetectorParameters
        );
    }

    /**
     * Separate the provided overlapping globules with a watershed segmentation.
     *
     * @param overlappingGlobules  the overlapping globules to separate
     * @param numberOfRows  the number of rows of the image containing the globules
     * @param numberOfColumns  the number of columns of the image containing the globules
     * @return a list of contours (as defined by OpenCV) of separated globules
     */
    private static List<Mat> separateOverlappingGlobules(List<Mat> overlappingGlobules, int numberOfRows, int numberOfColumns) {
        try (Mat mask = new Mat(numberOfRows, numberOfColumns, opencv_core.CV_8U)) {
            drawContours(mask, overlappingGlobules);

            return separateObjects(mask);
        }
    }

    /**
     * Filter the provided contours to keep only isolated globules.
     * The filtered globules are closed.
     *
     * @param contours  the contours to filter
     * @param fatGlobulesDetectorParameters  the parameters (solidity, elongation, and diameters) to use during filtering
     * @return a list of contours (as defined by OpenCV) of isolated globules
     */
    private static List<Mat> getIsolatedGlobules(List<Mat> contours, FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        Map<GlobuleClassification, List<Mat>> classifications = filterGlobules(contours, fatGlobulesDetectorParameters);
        for (Mat contour: classifications.get(GlobuleClassification.OVERLAPPING_GLOBULE)) {
            contour.close();
        }

        return classifications.get(GlobuleClassification.ISOLATED_GLOBULE);
    }

    /**
     * Create PathObjects from the provided list of globules.
     *
     * @param globules  the globules whose geometry is used to create the PathObjects
     * @param objectToCreate  the type of PathObjects to create
     * @param regionRequest  the region containing the provided globules
     * @return a list of PathObjects representing the provided list of globules
     */
    private static List<PathObject> createPathObjects(List<Mat> globules, FatGlobulesDetectorParameters.ObjectToCreate objectToCreate, RegionRequest regionRequest) {
        return globules.stream()
                .map(globule -> {
                    try (IntRawIndexer indexer = globule.createIndexer()) {
                        return ROIs.createPolygonROI(
                                IntStream.range(0, globule.size(0))
                                        .mapToObj(i -> new Point2(
                                                indexer.get(i, 0, 0) * regionRequest.getDownsample() + regionRequest.getX(),
                                                indexer.get(i, 0, 1) * regionRequest.getDownsample() + regionRequest.getY()
                                        ))
                                        .toList(),
                                regionRequest.getImagePlane()
                        );
                    }
                })
                .map(roi -> switch (objectToCreate) {
                    case ANNOTATION -> PathObjects.createAnnotationObject(roi);
                    case DETECTION -> PathObjects.createDetectionObject(roi);
                })
                .toList();

    }

    /**
     * Find the contours of a mask. This basically calls {@link OpenCVTools#createROIs(Mat, RegionRequest, int, int)}
     * and converts the result to a list of OpenCV {@link Mat} describing contours.
     *
     * @param mask  the mask whose contours should be retrieved
     * @return a list of contours as defined by OpenCV
     */
    private static List<Mat> findContours(Mat mask) {
        return OpenCVTools.createROIs(mask, null, 1, -1).values().stream()
                .map(RoiTools::splitROI)
                .flatMap(List::stream)
                .map(roi -> {
                    Mat contour = new Mat(roi.getAllPoints().size(), 1, opencv_core.SCALAR);

                    try (IntRawIndexer indexer = contour.createIndexer()) {
                        for (int i=0; i<contour.size(0); ++i) {
                            indexer.put(i, 0, 0, (int) roi.getAllPoints().get(i).getX());
                            indexer.put(i, 0, 1, (int) roi.getAllPoints().get(i).getY());
                        }
                    }

                    return contour;
                })
                .toList();
    }

    /**
     * <p>
     *     Filter a list of contours to find globules based on their solidity, elongation, and diameters
     * </p>
     * <p>
     *     A contour will either be:
     *     <ul>
     *         <li>Classified as an isolated globule.</li>
     *         <li>Classified as overlapping globules.</li>
     *         <li>Discarded. In that case, the underlying {@link Mat} is closed.</li>
     *     </ul>
     * </p>
     *
     * @param contours  the list of contours (as specified by OpenCV) to filter. Filtered contours will be closed.
     * @param fatGlobulesDetectorParameters  the parameters (solidity, elongation, and diameters) to use during filtering
     * @return a map containing the input contours classified as isolated or overlapping globules
     */
    private static Map<GlobuleClassification, List<Mat>> filterGlobules(List<Mat> contours, FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        double scale = fatGlobulesDetectorParameters.getImageData().getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

        Map<GlobuleClassification, List<Mat>> classifications = Map.of(
                GlobuleClassification.ISOLATED_GLOBULE, new ArrayList<>(),
                GlobuleClassification.OVERLAPPING_GLOBULE, new ArrayList<>()
        );

        for (Mat contour: contours) {
            try (Point2f center = new Point2f(0, 0)) {
                float[] radius = new float[] {0};
                try {
                    opencv_imgproc.minEnclosingCircle(contour, center, radius);
                } catch (RuntimeException ignored) {}
                double diameter = radius[0] * 2 * scale;
                double elongation = getElongation(contour);
                double solidity = getSolidity(contour);

                boolean isIsolatedGlobule = elongation > fatGlobulesDetectorParameters.getMinIsolatedGlobuleElongation() &&
                        solidity > fatGlobulesDetectorParameters.getMinIsolatedGlobuleSolidity() &&
                        fatGlobulesDetectorParameters.getMinDiameter() < diameter &&
                        diameter < fatGlobulesDetectorParameters.getMaxDiameter();
                boolean isOverlappingGlobules = elongation > fatGlobulesDetectorParameters.getMinOverlappingGlobuleElongation() &&
                        solidity > fatGlobulesDetectorParameters.getMinOverlappingGlobuleSolidity() &&
                        diameter > fatGlobulesDetectorParameters.getMinDiameter();

                if (isIsolatedGlobule) {
                    classifications.get(GlobuleClassification.ISOLATED_GLOBULE).add(contour);
                } else if (isOverlappingGlobules) {
                    classifications.get(GlobuleClassification.OVERLAPPING_GLOBULE).add(contour);
                } else {
                    contour.close();
                }
            }
        }

        return classifications;
    }

    /**
     * <p>
     *     Draw the provided list of contours to the provided mask.
     *     The contour interiors are drawn.
     * </p>
     * <p>
     *     The contours are drawn using the <b>ImageJ</b> way (and not the <b>OpenCV</b> way). See
     *     <a href="https://petebankhead.github.io/qupath/technical/2018/03/13/note-on-contours.html">this link</a>
     *     for more information.
     * </p>
     *
     * @param mask  the mask to draw the contours to
     * @param contours  the contours to draw on the mask
     */
    private static void drawContours(Mat mask, List<Mat> contours) {
        try (
                Mat upSampledMask = new Mat(mask.rows()*2, mask.cols()*2, mask.type());
                Size size = new Size(mask.cols(), mask.rows());
                Size upSampledSize = new Size(mask.cols()*2, mask.rows()*2)
        ) {
            opencv_imgproc.resize(mask, upSampledMask, upSampledSize, 0, 0, opencv_imgproc.INTER_NEAREST);

            for (Mat contour: contours) {
                Mat[] contoursArray = new Mat[] {contour};

                try (
                        IntRawIndexer indexer = contour.createIndexer();
                        MatVector contourVector = new MatVector(contoursArray);
                        Scalar color = new Scalar(255);
                        Mat hierarchy = new Mat();
                        Point offset = new Point(0, 0)
                ) {
                    for (int i=0; i<indexer.size(0); ++i) {
                        indexer.put(i, 0, 0, indexer.get(i, 0, 0) * 2 - 1);
                        indexer.put(i, 0, 1, indexer.get(i, 0, 1) * 2 - 1);
                    }

                    opencv_imgproc.drawContours(
                            upSampledMask,
                            contourVector,
                            -1,
                            color,
                            -1,
                            opencv_imgproc.LINE_8,
                            hierarchy,
                            Integer.MAX_VALUE,
                            offset
                    );
                }
            }

            opencv_imgproc.resize(upSampledMask, mask, size, 0, 0, opencv_imgproc.INTER_NEAREST);
        }
    }

    /**
     * Separate objects of a mask into a list of OpenCV contours using a Watershed segmentation.
     *
     * @param mask  the mask containing the objects to separate. This mask will be modified.
     * @return a list of contours (as defined by OpenCV) of objects identified by the watershed segmentation
     */
    private static List<Mat> separateObjects(Mat mask) {
        try (
                Mat labels = OpenCVTools.label(mask, 4);
                Indexer labelsIndexer = labels.createIndexer();
                Indexer maskIndexer = mask.createIndexer()
        ) {
            ProcessingCV.doWatershed(mask, labels, 1,false);
            OpenCVTools.apply(mask, d -> 0);

            long numberOfPixelsInMask = Arrays.stream(maskIndexer.sizes()).reduce(1, (a, b) -> a * b);
            Indexer maskIndexerFlattened = maskIndexer.reindex(Index.create(numberOfPixelsInMask));
            Indexer labelsIndexerFlattened = labelsIndexer.reindex(Index.create(numberOfPixelsInMask));

            Map<Integer, List<Integer>> labelToCoordinates = new HashMap<>();
            for (int coordinate = 0; coordinate < numberOfPixelsInMask; coordinate++) {
                int label = (int) labelsIndexerFlattened.getDouble(new long[] {coordinate});

                if (labelToCoordinates.containsKey(label)) {
                    labelToCoordinates.get(label).add(coordinate);
                } else {
                    List<Integer> newList = new ArrayList<>();
                    newList.add(coordinate);
                    labelToCoordinates.put(label, newList);
                }
            }

            double maxLabel = OpenCVTools.maximum(labels);
            List<Mat> contours = new ArrayList<>();
            for (int label=1; label<maxLabel+1; ++label) {
                if (labelToCoordinates.containsKey(label)) {
                    for (int coordinate: labelToCoordinates.get(label)) {
                        maskIndexerFlattened.putDouble(new long[] {coordinate}, 255);
                    }

                    contours.addAll(findContours(mask));

                    for (int coordinate: labelToCoordinates.get(label)) {
                        maskIndexerFlattened.putDouble(new long[] {coordinate}, 0);
                    }
                }
            }
            return contours;
        }
    }

    /**
     * Compute the elongation of a shape described as a contour.
     *
     * @param contour  the shape (described as a contour) whose elongation should be computed
     * @return the elongation of the provided shape
     */
    private static double getElongation(Mat contour) {
        try (Moments moments = opencv_imgproc.moments(contour)) {
            double x = moments.mu20() + moments.mu02();
            double y = Math.sqrt(4 * Math.pow(moments.mu11(), 2) + Math.pow(moments.mu20() - moments.mu02(), 2));

            return (x - y) / (x + y);
        } catch (Exception e) {
            logger.warn("Error when computing elongation", e);
            return 0;
        }
    }

    /**
     * Compute the solidity of a shape described as a contour.
     *
     * @param contour  the shape (described as a contour) whose solidity should be computed
     * @return the solidity of the provided shape
     */
    private static double getSolidity(Mat contour) {
        try (Mat hull = new Mat()) {
            opencv_imgproc.convexHull(contour, hull);
            double hull_area = opencv_imgproc.contourArea(hull);
            double area = opencv_imgproc.contourArea(contour);
            return area / hull_area;
        } catch (Exception e) {
            logger.warn("Error when computing solidity", e);
            return 0;
        }
    }
}
