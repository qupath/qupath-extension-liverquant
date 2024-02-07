import qupath.ext.liverquant.core.*

/*
 * This script runs the globule detection with custom parameters.
 *
 * An image must be currently opened in QuPath through the QuPath GUI or
 * through the command line and at least one annotation must be selected.
 *
 * If you execute this script through the script editor, you might have to
 * click on Objects > Refresh Object Ids to see the results.
 */

def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image must be open before running this script"
    return
}

def selectedAnnotations = getSelectedObjects()      // The annotations to analyze. Other annotations could be selected
if (selectedAnnotations.isEmpty()) {
    println "You must select at least one annotation before running the detection"
    return
}

FatGlobuleDetector.run(new FatGlobulesDetectorParameters.Builder(imageData, selectedAnnotations)
        .setProgressDisplay(FatGlobulesDetectorParameters.ProgressDisplay.WINDOW)       // or FatGlobulesDetectorParameters.ProgressDisplay.LOG to not use the UI
        .setObjectToCreate(FatGlobulesDetectorParameters.ObjectToCreate.DETECTION)      // or FatGlobulesDetectorParameters.ObjectToCreate.ANNOTATION to create annotations instead of detections
        .setPixelSize(-1)          // pixel size in microns at which the detection should be performed. A negative value means using the full resolution image
        .setLowerBound(new FatGlobulesDetectorParameters.HsvArray(0, 0, 200))   // the inclusive lower bound array in HSV-space that should be used for color segmentation
        .setUpperBound(new FatGlobulesDetectorParameters.HsvArray(180, 25, 255))    // the inclusive upper bound array in HSV-space that should be used for color segmentation
        .setMinIsolatedGlobuleElongation(0.4)   // the minimal elongation a shape should have to be considered as an isolated globule
        .setMinOverlappingGlobuleElongation(0.05)   // the minimal elongation a shape should have to be considered as an overlapping globule
        .setMinIsolatedGlobuleSolidity(0.85)    // the minimal solidity a shape should have to be considered as an isolated globule
        .setMinOverlappingGlobuleSolidity(0.7)  // the minimal solidity a shape should have to be considered as an overlapping globule
        .setMinDiameter(5)      // the minimal diameter (in microns) a shape should have to be considered as a globule
        .setMaxDiameter(100)    // the maximal diameter (in microns) a shape should have to be considered as an isolated globule
        .setTileWidth(512)      // the width of each tile (in pixels) at which the detection should be performed
        .setTileHeight(512)     // the height of each tile (in pixel) at which the detection should be performed
        .setPadding(64)         // the padding (in pixels) of each tile at which the detection should be performed
        .setBoundaryThreshold(0.5)  // Objects created on the boundaries of tiles are merged with a shared boundary IoU criterion. The value passed to this function is the minimum intersection-over-union (IoU) proportion of the possibly-clipped boundary for merging
        .setOnFinished(() -> {
            // Everything here will be executed after the detection is complete.
            // Note that if you print something here, you'll have to open the QuPath logs to see it (it won't show on the script editor)
        })
        .build()
)