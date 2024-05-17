import qupath.ext.liverquant.core.*

/*
 * This script runs the globule detection with default parameters.
 * Take a look at the run-detection-with-parameters.groovy script to
 * see how to define custom parameters.
 *
 * An image must be currently opened in QuPath through the QuPath GUI or
 * through the command line.
 *
 * If you execute this script through the script editor, you might have to
 * click on Objects > Refresh Object Ids to see the results.
 */

def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image must be open before running this script"
    return
}

FatGlobuleDetector.run(new FatGlobulesDetectorParameters.Builder(imageData).build())