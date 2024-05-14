package qupath.ext.liverquant.core;

import org.bytedeco.javacpp.indexer.Index;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatExpr;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Moments;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.RoiTools;
import qupath.opencv.tools.OpenCVTools;
import qupath.opencv.tools.ProcessingCV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MatOperations {

    private static final Logger logger = LoggerFactory.getLogger(MatOperations.class);

    private MatOperations() {
        throw new AssertionError("This class is not instantiable.");
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
    public static void segmentByColor(Mat mat, FatGlobulesDetectorParameters.HsvArray lowerBound, FatGlobulesDetectorParameters.HsvArray upperBound) {
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
     * Add a border of one pixel with value 255 to the provided image.
     *
     * @param mat  the image to add a border to
     */
    public static void addBorder(Mat mat) {
        try (Scalar scalar = new Scalar(255)) {
            opencv_core.copyMakeBorder(mat, mat, 1, 1, 1, 1, opencv_core.BORDER_CONSTANT, scalar);
        }
    }

    /**
     * Fill some holes of a mask with white pixels. The provided mask will be modified.
     *
     * @param mask  the mask containing the holes to fill. It must have the {@link opencv_core#CV_8U} format
     * @param holeSize  the maximal size (area) a hole can have to be filled
     * @param resolution  the pixel resolution of the mask in microns
     */
    public static void fillHoles(Mat mask, double holeSize, double resolution) {
        try (
                MatVector contours = new MatVector();
                Mat hierarchy = new Mat()
        ) {
            opencv_imgproc.findContours(mask, contours, hierarchy, opencv_imgproc.RETR_CCOMP, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            try (IntRawIndexer indexer = hierarchy.createIndexer()) {
                for (int i=0; i<contours.size(); ++i) {
                    if (indexer.get(0, i, 3) > -1 &&
                            holeSize < 0 || opencv_imgproc.contourArea(contours.get(i)) * resolution * resolution < holeSize
                    ) {
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
     * Fill all holes of a mask with white pixels. The provided mask will be modified.
     *
     * @param mask  the mask containing the holes to fill. It must have the {@link opencv_core#CV_8U} format
     */
    public static void fillHoles(Mat mask) {
        fillHoles(mask, -1, 0);
    }

    /**
     * Find the contours of a mask. This basically calls {@link OpenCVTools#createROIs(Mat, RegionRequest, int, int)}
     * and converts the result to a list of OpenCV {@link Mat} describing contours.
     *
     * @param mask  the mask whose contours should be retrieved
     * @return a list of contours as defined by OpenCV
     */
    public static List<Mat> findContours(Mat mask) {
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
    public static void drawContours(Mat mask, List<Mat> contours) {
        try (
                MatExpr upSampledMaskEpr = Mat.zeros(mask.rows()*2, mask.cols()*2, mask.type());
                Mat upSampledMask = upSampledMaskEpr.asMat();
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
    public static List<Mat> separateObjects(Mat mask) {
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
    public static double getElongation(Mat contour) {
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
    public static double getSolidity(Mat contour) {
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
