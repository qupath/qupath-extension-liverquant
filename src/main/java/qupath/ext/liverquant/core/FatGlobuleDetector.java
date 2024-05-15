package qupath.ext.liverquant.core;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatExpr;
import org.bytedeco.opencv.opencv_core.Point2f;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;


/**
 * Detect fat globules within image tiles based on
 * <a href="https://github.com/mfarzi/liverquant">liverquant</a>.
 */
public class FatGlobuleDetector {

    private enum GlobuleClassification {
        ISOLATED_GLOBULE,
        OVERLAPPING_GLOBULE
    }

    private FatGlobuleDetector() {
        throw new AssertionError("This class is not instantiable.");
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

                    MatOperations.segmentByColor(
                            mat,
                            fatGlobulesDetectorParameters.getLowerBound(),
                            fatGlobulesDetectorParameters.getUpperBound()
                    );
                    MatOperations.fillHoles(mat);

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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            processor.processObjects(
                    switch (fatGlobulesDetectorParameters.getProgressDisplay()) {
                        case WINDOW -> new TaskRunnerFX(QuPathGUI.getInstance());
                        case LOG -> new CommandLineTaskRunner();
                    },
                    fatGlobulesDetectorParameters.getImageData(),
                    switch (fatGlobulesDetectorParameters.getDetectionRegion()) {
                        case SELECTED_ANNOTATIONS -> fatGlobulesDetectorParameters.getAnnotations();
                        case DETECTED_TISSUE -> {
                            List<PathObject> annotations = TissueDetector.detectTissue(fatGlobulesDetectorParameters.getTissueDetectorParameters());
                            fatGlobulesDetectorParameters.getImageData().getHierarchy().addObjects(annotations);
                            yield annotations;
                        }
                    }
            );

            fatGlobulesDetectorParameters.getOnFinished().run();
        });
        executor.shutdown();
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
                MatOperations.findContours(mask),
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
        try (
                MatExpr maskExpr = Mat.zeros(numberOfRows, numberOfColumns, opencv_core.CV_8U);
                Mat mask = maskExpr.asMat()
        ) {
            MatOperations.drawContours(mask, overlappingGlobules);

            return MatOperations.separateObjects(mask);
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
                double elongation = MatOperations.getElongation(contour);
                double solidity = MatOperations.getSolidity(contour);

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
}
