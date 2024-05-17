import qupath.ext.liverquant.core.*

/*
 * This script runs the globule detection with custom parameters.
 *
 * An image must be currently opened in QuPath through the QuPath GUI or
 * through the command line.
 *
 * If you execute this script through the script editor, you might have to
 * click on Objects > Refresh Object Ids to see the results.
 */

// Parameters. The values displayed here are the default values, so if nothing is modified,
// running this script is equivalent to running the run-detection.groovy script
def detectionRegion = FatGlobulesDetectorParameters.DetectionRegion.DETECTED_TISSUE     // An algorithm to detect the tissue will be run on the entire image before the fat globule detection.
                                                                                        // Can be FatGlobulesDetectorParameters.DetectionRegion.SELECTED_ANNOTATIONS to skip this tissue
                                                                                        // detection and perform the fat globule detection within the annotations defined below
def annotations = []       // Define the annotations where to perform the fat globule detection, for example getSelectedObjects(). This parameter is only taken
                                    // into account if the detectionRegion variable above is defined to FatGlobulesDetectorParameters.DetectionRegion.SELECTED_ANNOTATIONS
def progressDisplay = FatGlobulesDetectorParameters.ProgressDisplay.WINDOW      // or FatGlobulesDetectorParameters.ProgressDisplay.LOG to not use the UI
def objectToCreate = FatGlobulesDetectorParameters.ObjectToCreate.DETECTION     // or FatGlobulesDetectorParameters.ObjectToCreate.ANNOTATION to create annotations instead of detections
def pixelSize = -1                                                  // pixel size in microns at which the detection should be performed. A negative value means using the full resolution image
def lowerBound = new HsvArray(0, 0, 200)               // the inclusive lower bound array in HSV-space that should be used for color segmentation
def upperBound = new HsvArray(180, 25, 255)            // the inclusive upper bound array in HSV-space that should be used for color segmentation
def minIsolatedGlobuleElongation = 0.4      // the minimal elongation a shape should have to be considered as an isolated globule
def minOverlappingGlobuleElongation = 0.05  // the minimal elongation a shape should have to be considered as an overlapping globule
def minIsolatedGlobuleSolidity = 0.85       // the minimal solidity a shape should have to be considered as an isolated globule
def minOverlappingGlobuleSolidity = 0.7     // the minimal solidity a shape should have to be considered as an overlapping globule
def minDiameter = 5                            // the minimal diameter (in microns) a shape should have to be considered as a globule
def maxDiameter = 100                          // the maximal diameter (in microns) a shape should have to be considered as an isolated globule
def tileWidth = 512                            // the width of each tile (in pixels) at which the detection should be performed
def tileHeight = 512                           // the height of each tile (in pixels) at which the detection should be performed
def padding = 64                               // the padding (in pixels) of each tile at which the detection should be performed
def boundaryThreshold = 0.5                 // Objects created on the boundaries of tiles are merged with a shared boundary IoU criterion.
                                                       // The boundary threshold is the minimum intersection-over-union (IoU) proportion
                                                       // of the possibly-clipped boundary for merging
def onFinished = () -> {
    // Everything here will be executed after the detection is complete.
    // Note that if you print something here, you'll have to open the QuPath logs to see it (it won't show on the script editor)
}
// All parameters below are only taken into account if the detectionRegion variable above is defined to FatGlobulesDetectorParameters.DetectionRegion.DETECTED_TISSUE.
// They control the tissue detection (and not the fat globule detection)
def lowerBoundTissueDetection = new HsvArray(0, 0, 200)               // the inclusive lower bound array in HSV-space that should be used for color segmentation
                                                                      // during the tissue detection
def upperBoundTissueDetection = new HsvArray(180, 10, 255)            // the inclusive upper bound array in HSV-space that should be used for color segmentation
                                                                      // during the tissue detection
def downsample = 32         // the downsample to use on the image when performing the tissue detection
def minTissueArea = 5e5  // the minimum area a tissue should have (in micro-meter squared)


def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image must be open before running this script"
    return
}

FatGlobuleDetector.run(new FatGlobulesDetectorParameters.Builder(imageData)
        .setTissueDetectorParameters(new TissueDetectorParameters.Builder(imageData.getServer())
                .setLowerBound(lowerBoundTissueDetection)
                .setUpperBound(upperBoundTissueDetection)
                .setDownsample(downsample)
                .setMinTissueArea(minTissueArea)
                .build()
        )
        .setAnnotations(annotations)
        .setDetectionRegion(detectionRegion)
        .setProgressDisplay(progressDisplay)
        .setObjectToCreate(objectToCreate)
        .setPixelSize(pixelSize)
        .setLowerBound(lowerBound)
        .setUpperBound(upperBound)
        .setMinIsolatedGlobuleElongation(minIsolatedGlobuleElongation)
        .setMinOverlappingGlobuleElongation(minOverlappingGlobuleElongation)
        .setMinIsolatedGlobuleSolidity(minIsolatedGlobuleSolidity)
        .setMinOverlappingGlobuleSolidity(minOverlappingGlobuleSolidity)
        .setMinDiameter(minDiameter)
        .setMaxDiameter(maxDiameter)
        .setTileWidth(tileWidth)
        .setTileHeight(tileHeight)
        .setPadding(padding)
        .setBoundaryThreshold(boundaryThreshold)
        .setOnFinished(onFinished)
        .build()
)