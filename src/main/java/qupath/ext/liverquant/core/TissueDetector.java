package qupath.ext.liverquant.core;

import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.regions.RegionRequest;
import qupath.opencv.tools.OpenCVTools;

import java.io.IOException;
import java.util.List;

/**
 * Detect tissue within an image based on
 * <a href="https://github.com/mfarzi/liverquant">liverquant</a>.
 */
public class TissueDetector {

    private static final Logger logger = LoggerFactory.getLogger(FatGlobuleDetector.class);

    /**
     * Run the detection algorithm.
     *
     * @param tissueDetectorParameters  the parameters to use for the detection
     * @return a list of annotations containing the tissue of the image
     */
    public static List<PathObject> detectTissue(TissueDetectorParameters tissueDetectorParameters) {
        RegionRequest regionRequest = RegionRequest.createInstance(tissueDetectorParameters.getServer(), tissueDetectorParameters.getDownsample());

        Mat mat;
        try {
            mat = OpenCVTools.imageToMat(tissueDetectorParameters.getServer().readRegion(regionRequest));
        } catch (IOException e) {
            logger.error("Error when reading image", e);
            return List.of();
        }

        MatOperations.segmentByColor(mat, tissueDetectorParameters.getLowerBound(), tissueDetectorParameters.getUpperBound());
        MatOperations.addBorder(mat);
        MatOperations.fillHoles(
                mat,
                tissueDetectorParameters.getMinTissueArea() /
                        Math.pow(tissueDetectorParameters.getServer().getPixelCalibration().getAveragedPixelSizeMicrons() * tissueDetectorParameters.getDownsample(), 2)
        );
        mat = MatOperations.removeBorder(mat);
        MatOperations.bitwiseNot(mat);
        MatOperations.fillHoles(mat);

        List<PathObject> annotations = OpenCVTools.createROIs(mat, regionRequest, 1, -1).values().stream()
                .map(PathObjects::createAnnotationObject)
                .peek(annotation -> annotation.setLocked(true))
                .toList();

        mat.close();

        return annotations;
    }
}
