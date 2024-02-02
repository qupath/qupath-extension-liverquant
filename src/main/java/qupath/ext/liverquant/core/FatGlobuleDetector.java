package qupath.ext.liverquant.core;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.experimental.pixels.OpenCVProcessor;
import qupath.lib.experimental.pixels.OutputHandler;
import qupath.lib.experimental.pixels.Parameters;
import qupath.lib.geom.Point2;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.interfaces.ROI;
import qupath.opencv.tools.OpenCVTools;

import java.util.List;
import java.util.Map;

public class FatGlobuleDetector {

    private static final Logger logger = LoggerFactory.getLogger(FatGlobuleDetector.class);
    private enum Classification {
        GLOBULE,
        UNKNOWN
    }

    public static void run(FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        OpenCVProcessor.builder(params -> {
            Mat mat = params.getImage().clone();

            segmentByColor(mat, fatGlobulesDetectorParameters);
            fillHoles(mat);

            return mat;
        })
                .downsample(
                        fatGlobulesDetectorParameters.getPixelSize() / fatGlobulesDetectorParameters.getImageData().getServer().getPixelCalibration().getAveragedPixelSizeMicrons()
                )
                .tile(fatGlobulesDetectorParameters.getTileWidth(), fatGlobulesDetectorParameters.getTileHeight())
                .padding(fatGlobulesDetectorParameters.getPadding())
                .mergeSharedBoundaries(fatGlobulesDetectorParameters.getBoundaryThreshold())
                .outputHandler(OutputHandler.createObjectOutputHandler((Parameters<Mat, Mat> params, Mat output) -> {
                    // Step 1
                    List<Mat> contours = List.of();
                    try (Mat mat = output.clone()) {
                        contours = findContours(mat, params.getRegionRequest());
                    }

                    Map<Classification, List<Mat>> classifications = filterGlobules(contours, fatGlobulesDetectorParameters);
                    List<Mat> globules = classifications.get(Classification.GLOBULE);
                    List<Mat> unknown = classifications.get(Classification.UNKNOWN);




                    // Step 2
                    Mat mask = Mat.zeros(output.rows(), output.cols(), output.type()).asMat()
                    draw_geocontours(mask, unknown)
                    for (Mat contour: unknown) {
                        contour.close()
                    }
                    List<Mat> separated_globules = separate_globules(mask, params.regionRequest)
                    mask.close()

                    // Step 3
                    def filterSeparatedGlobules = filter_fat_globules(
                            separated_globules,
                            cal,
                            minFatGlobuleElongation,
                            minOverlappingFatGlobuleElongation,
                            minFatGlobuleSolidity,
                            minOverlappingFatGlobuleSolidity,
                            minDiameter,
                            maxDiameter,
                            downsample
                    )
                    globules.addAll(filterSeparatedGlobules.get(0))
                    for (Mat contour: filterSeparatedGlobules.get(1)) {
                        contour.close()
                    }

                    // Create detections
                    def rois = []
                    for (Mat globule: globules) {
                        try (IntRawIndexer indexer = globule.createIndexer()) {
                            List<Point2> points = []
                            for (int i=0; i<globule.size(0); ++i) {
                                points.add(new Point2(
                                        indexer.get(i, 0, 0) * params.regionRequest.downsample + params.regionRequest.getX(),
                                        indexer.get(i, 0, 1) * params.regionRequest.downsample + params.regionRequest.getY()
                                ))
                            }

                            rois.add(ROIs.createPolygonROI(points, params.regionRequest.getImagePlane()))
                        }

                        globule.close()
                    }

                    def newObjects = []
                    for (ROI roiDetected : rois) {
                        def newObject = objectCreator.apply(roiDetected)
                        newObject.setLocked(true)
                        // In case we want to check this later
                        newObject.measurements['Area ' + cal.getPixelWidthUnit() + "²"] = roiDetected.getScaledArea(cal.pixelWidth as double, cal.pixelHeight as double)
                        newObject.measurements['Solidity'] = roiDetected.getSolidity()
                        //newObject.measurements['Aspect ratio'] = getAspectRatio(roiDetected)
                        newObjects << newObject
                    }

                    return newObjects
                }))
                .build();
    }

    private static void segmentByColor(Mat mat, FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        try (
                Scalar lowerBoundScalar = new Scalar(
                        fatGlobulesDetectorParameters.getLowerBoundHue(),
                        fatGlobulesDetectorParameters.getLowerBoundSaturation(),
                        fatGlobulesDetectorParameters.getLowerBoundValue(),
                        255
                );
                Mat lowerBound = new Mat(lowerBoundScalar);
                Scalar upperBoundScalar = new Scalar(
                        fatGlobulesDetectorParameters.getUpperBoundHue(),
                        fatGlobulesDetectorParameters.getUpperBoundSaturation(),
                        fatGlobulesDetectorParameters.getUpperBoundValue(),
                        255
                );
                Mat upperBound = new Mat(upperBoundScalar)
        ) {
            mat.convertTo(mat, opencv_core.CV_8U);
            opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_RGB2HSV);
            opencv_core.inRange(mat, lowerBound, upperBound, mat);
        }
    }

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

    private static List<Mat> findContours(Mat mask, RegionRequest request) {
        return OpenCVTools.createROIs(mask, request, 1, -1).values().stream()
                .map(RoiTools::splitROI)
                .flatMap(List::stream)
                .map(roi -> {
                    Mat contour = Mat.zeros(roi.getAllPoints().size(), 1,12).asMat();

                    try (IntRawIndexer indexer = contour.createIndexer()) {
                        for (int i=0; i<contour.size(0); ++i) {
                            indexer.put(i, 0, 0, (int) ((roi.getAllPoints().get(i).getX() - request.getX()) / request.getDownsample()));
                            indexer.put(i, 0, 1, (int) ((roi.getAllPoints().get(i).getY() - request.getY()) / request.getDownsample()));
                        }
                    }

                    return contour;
                })
                .toList();
    }

    private static Map<Classification, List<Mat>> filterGlobules(List<Mat> contours, FatGlobulesDetectorParameters fatGlobulesDetectorParameters) {
        double scale = fatGlobulesDetectorParameters.getImageData().getServer().getPixelCalibration().getAveragedPixelSizeMicrons();

        Map<Classification, List<Mat>> classifications = Map.of(
                Classification.GLOBULE, List.of(),
                Classification.UNKNOWN, List.of()
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

                boolean isFatGlobule = elongation > fatGlobulesDetectorParameters.getMinFatGlobuleElongation() &&
                        solidity > fatGlobulesDetectorParameters.getMinFatGlobuleSolidity() &&
                        fatGlobulesDetectorParameters.getMinDiameter() < diameter &&
                        diameter < fatGlobulesDetectorParameters.getMaxDiameter();
                boolean isUnknown = elongation > fatGlobulesDetectorParameters.getMinOverlappingFatGlobuleElongation() &&
                        solidity > fatGlobulesDetectorParameters.getMinOverlappingFatGlobuleSolidity() &&
                        diameter > fatGlobulesDetectorParameters.getMinDiameter();

                if (isFatGlobule) {
                    classifications.get(Classification.GLOBULE).add(contour);
                } else if (isUnknown) {
                    classifications.get(Classification.UNKNOWN).add(contour);
                } else {
                    contour.close();
                }
            }
        }

        return classifications;
    }

    private static double getElongation(Mat mat) {
        try (Moments moments = opencv_imgproc.moments(mat)) {
            double x = moments.mu20() + moments.mu02();
            double y = Math.sqrt(4 * Math.pow(moments.mu11(), 2) + Math.pow(moments.mu20() - moments.mu02(), 2));

            return (x - y) / (x + y);
        } catch (Exception e) {
            logger.warn("Error when computing elongation", e);
            return 0;
        }
    }

    private static double getSolidity(Mat mat) {
        try (Mat hull = new Mat()) {
            opencv_imgproc.convexHull(mat, hull);
            double hull_area = opencv_imgproc.contourArea(hull);
            double area = opencv_imgproc.contourArea(mat);
            return area / hull_area;
        } catch (Exception e) {
            logger.warn("Error when computing solidity", e);
            return 0;
        }
    }
}
