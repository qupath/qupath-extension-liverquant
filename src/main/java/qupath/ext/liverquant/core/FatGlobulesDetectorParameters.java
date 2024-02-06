package qupath.ext.liverquant.core;

import qupath.ext.liverquant.gui.UiUtilities;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * <p>
 *     Parameters to start a {@link FatGlobuleDetector}.
 * </p>
 * <p>
 *     Use the {@link Builder} to create an instance of this object.
 * </p>
 */
public class FatGlobulesDetectorParameters {

    private final ImageData<BufferedImage> imageData;
    private final Collection<PathObject> annotations;
    private final ProgressDisplay progressDisplay;
    private final ObjectToCreate objectToCreate;
    private final float pixelSize;
    private final HsvArray lowerBound;
    private final HsvArray upperBound;
    private final float minIsolatedGlobuleElongation;
    private final float minOverlappingGlobuleElongation;
    private final float minIsolatedGlobuleSolidity;
    private final float minOverlappingGlobuleSolidity;
    private final float minDiameter;
    private final float maxDiameter;
    private final int tileWidth;
    private final int tileHeight;
    private final int padding;
    private final float boundaryThreshold;
    private final Runnable onFinished;
    /**
     * Define the type of object to create to represent a globule
     */
    public enum ObjectToCreate {
        ANNOTATION,
        DETECTION
    }
    /**
     * Define how to display the progress of the algorithm
     */
    public enum ProgressDisplay {
        WINDOW,
        LOG
    }

    private FatGlobulesDetectorParameters(Builder builder) {
        this.imageData = builder.imageData;
        this.annotations = builder.annotations;
        this.progressDisplay = builder.progressDisplay;
        this.objectToCreate = builder.objectToCreate;
        this.pixelSize = builder.pixelSize;
        this.lowerBound = builder.lowerBound;
        this.upperBound = builder.upperBound;
        this.minIsolatedGlobuleElongation = builder.minIsolatedGlobuleElongation;
        this.minOverlappingGlobuleElongation = builder.minOverlappingGlobuleElongation;
        this.minIsolatedGlobuleSolidity = builder.minIsolatedGlobuleSolidity;
        this.minOverlappingGlobuleSolidity = builder.minOverlappingGlobuleSolidity;
        this.minDiameter = builder.minDiameter;
        this.maxDiameter = builder.maxDiameter;
        this.tileWidth = builder.tileWidth;
        this.tileHeight = builder.tileHeight;
        this.padding = builder.padding;
        this.boundaryThreshold = builder.boundaryThreshold;
        this.onFinished = builder.onFinished;
    }

    /**
     * @return the ImageData representing the image to use the algorithm on
     */
    public ImageData<BufferedImage> getImageData() {
        return imageData;
    }

    /**
     * @return the annotations that define the areas where the detection should take place
     */
    public Collection<PathObject> getAnnotations() {
        return annotations;
    }

    /**
     * @return the method to use to monitor progress
     */
    public ProgressDisplay getProgressDisplay() {
        return progressDisplay;
    }

    /**
     * @return the type of object to create when representing a globule
     */
    public ObjectToCreate getObjectToCreate() {
        return objectToCreate;
    }

    /**
     * @return the pixel size at which detection should be performed
     */
    public float getPixelSize() {
        return pixelSize;
    }

    /**
     * @return the inclusive lower bound array in HSV-space that should be used for color segmentation
     */
    public HsvArray getLowerBound() {
        return lowerBound;
    }

    /**
     * @return the inclusive upper bound array in HSV-space that should be used for color segmentation
     */
    public HsvArray getUpperBound() {
        return upperBound;
    }

    /**
     * @return the minimal elongation a shape should have to be considered as an isolated globule
     */
    public float getMinIsolatedGlobuleElongation() {
        return minIsolatedGlobuleElongation;
    }

    /**
     * @return the minimal elongation a shape should have to be considered as an overlapping globule
     */
    public float getMinOverlappingGlobuleElongation() {
        return minOverlappingGlobuleElongation;
    }

    /**
     * @return the minimal solidity a shape should have to be considered as an isolated globule
     */
    public float getMinIsolatedGlobuleSolidity() {
        return minIsolatedGlobuleSolidity;
    }

    /**
     * @return the minimal solidity a shape should have to be considered as an overlapping globule
     */
    public float getMinOverlappingGlobuleSolidity() {
        return minOverlappingGlobuleSolidity;
    }

    /**
     * @return the minimal diameter (in microns) a shape should have to be considered as a globule
     */
    public float getMinDiameter() {
        return minDiameter;
    }

    /**
     * @return the maximal diameter (in microns) a shape should have to be considered as an isolated globule
     */
    public float getMaxDiameter() {
        return maxDiameter;
    }

    /**
     * @return the width of each tile at which the detection should be performed
     */
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * @return the height of each tile at which the detection should be performed
     */
    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * @return the padding of each tile at which the detection should be performed
     */
    public int getPadding() {
        return padding;
    }

    /**
     * Objects created on the boundaries of tiles are merged with a shared boundary IoU criterion.
     * The value returned by this function is the minimum intersection-over-union (IoU) proportion
     * of the possibly-clipped boundary for merging (see {@link qupath.lib.objects.utils.ObjectMerger#createSharedTileBoundaryMerger(double)}
     *
     * @return the minimum intersection-over-union (IoU) proportion of the possibly-clipped boundary for merging
     */
    public float getBoundaryThreshold() {
        return boundaryThreshold;
    }

    /**
     * @return an operation to be run after the detection is complete
     */
    public Runnable getOnFinished() {
        return onFinished;
    }

    public record HsvArray(int hue, int saturation, int value) {

        /**
         * Define a pixel value in the HSV-space.
         *
         * @param hue  the hue of the pixel, between 0 and 180
         * @param saturation  the saturation of the pixel, between 0 and 255
         * @param value  the value of the pixel, between 0 and 255
         * @throws IllegalArgumentException if one of the value is not in the correct range
         */
        public HsvArray {
            if (hue < 0 || hue > 180) {
                throw new IllegalArgumentException(String.format("The supplied hue (%d) is not within the required range ([0, 180])", hue));
            }
            if (saturation < 0 || saturation > 255) {
                throw new IllegalArgumentException(String.format("The supplied saturation (%d) is not within the required range ([0, 255])", saturation));
            }
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException(String.format("The supplied value (%d) is not within the required range ([0, 255])", value));
            }
        }
    }

    /**
     * Create an instance of {@link FatGlobulesDetectorParameters}.
     */
    public static class Builder {

        private final ImageData<BufferedImage> imageData;
        private final Collection<PathObject> annotations;
        private ProgressDisplay progressDisplay = UiUtilities.usingGUI() ? ProgressDisplay.WINDOW : ProgressDisplay.LOG;
        private ObjectToCreate objectToCreate = ObjectToCreate.DETECTION;
        private float pixelSize = 0.5f;
        private HsvArray lowerBound = new HsvArray(0, 0, 200);
        private HsvArray upperBound = new HsvArray(180, 25, 255);
        private float minIsolatedGlobuleElongation = 0.4f;
        private float minOverlappingGlobuleElongation = 0.05f;
        private float minIsolatedGlobuleSolidity = 0.85f;
        private float minOverlappingGlobuleSolidity = 0.7f;
        private float minDiameter = 5;
        private float maxDiameter = 100;
        private int tileWidth = 512;
        private int tileHeight = 512;
        private int padding = 64;
        private float boundaryThreshold = 0.5f;
        private Runnable onFinished = () -> {};

        /**
         * Create the builder.
         *
         * @param imageData  the ImageData representing the image to use the algorithm on
         * @param annotations  the annotations that define the areas where the detection should take place
         */
        public Builder(ImageData<BufferedImage> imageData, Collection<PathObject> annotations) {
            this.imageData = imageData;
            this.annotations = annotations;
        }

        /**
         * @param progressDisplay  the method to use to monitor progress
         * @return this builder
         */
        public Builder setProgressDisplay(ProgressDisplay progressDisplay) {
            this.progressDisplay = progressDisplay;
            return this;
        }

        /**
         * @param objectToCreate  the type of object to create when representing a globule
         * @return this builder
         */
        public Builder setObjectToCreate(ObjectToCreate objectToCreate) {
            this.objectToCreate = objectToCreate;
            return this;
        }

        /**
         * @param pixelSize  the pixel size at which detection should be performed
         * @return this builder
         */
        public Builder setPixelSize(float pixelSize) {
            this.pixelSize = pixelSize;
            return this;
        }

        /**
         * @param lowerBound  the inclusive lower bound array in HSV-space that should be used for color segmentation
         * @return this builder
         */
        public Builder setLowerBound(HsvArray lowerBound) {
            this.lowerBound = lowerBound;
            return this;
        }

        /**
         * @param upperBound  the inclusive upper bound array in HSV-space that should be used for color segmentation
         * @return this builder
         */
        public Builder setUpperBound(HsvArray upperBound) {
            this.upperBound = upperBound;
            return this;
        }

        /**
         * @param minIsolatedGlobuleElongation  the minimal elongation a shape should have to be considered as an isolated globule
         * @return this builder
         */
        public Builder setMinIsolatedGlobuleElongation(float minIsolatedGlobuleElongation) {
            this.minIsolatedGlobuleElongation = minIsolatedGlobuleElongation;
            return this;
        }

        /**
         * @param minOverlappingGlobuleElongation  the minimal elongation a shape should have to be considered as an overlapping globule
         * @return this builder
         */
        public Builder setMinOverlappingGlobuleElongation(float minOverlappingGlobuleElongation) {
            this.minOverlappingGlobuleElongation = minOverlappingGlobuleElongation;
            return this;
        }

        /**
         * @param minIsolatedGlobuleSolidity  the minimal solidity a shape should have to be considered as an isolated globule
         * @return this builder
         */
        public Builder setMinIsolatedGlobuleSolidity(float minIsolatedGlobuleSolidity) {
            this.minIsolatedGlobuleSolidity = minIsolatedGlobuleSolidity;
            return this;
        }

        /**
         * @param minOverlappingGlobuleSolidity  the minimal solidity a shape should have to be considered as an overlapping globule
         * @return this builder
         */
        public Builder setMinOverlappingGlobuleSolidity(float minOverlappingGlobuleSolidity) {
            this.minOverlappingGlobuleSolidity = minOverlappingGlobuleSolidity;
            return this;
        }

        /**
         * @param minDiameter  the minimal diameter (in microns) a shape should have to be considered as a globule
         * @return this builder
         */
        public Builder setMinDiameter(float minDiameter) {
            this.minDiameter = minDiameter;
            return this;
        }

        /**
         * @param maxDiameter  the maximal diameter (in microns) a shape should have to be considered as an isolated globule
         * @return this builder
         */
        public Builder setMaxDiameter(float maxDiameter) {
            this.maxDiameter = maxDiameter;
            return this;
        }

        /**
         * @param tileWidth  the width of each tile (in pixels) at which the detection should be performed
         * @return this builder
         * @throws IllegalArgumentException if the tile width is negative
         */
        public Builder setTileWidth(int tileWidth) {
            if (tileWidth < 0) {
                throw new IllegalArgumentException(String.format("The supplied tile width (%d) is less than 0", tileWidth));
            }

            this.tileWidth = tileWidth;
            return this;
        }

        /**
         * @param tileHeight  the height of each tile (in pixel) at which the detection should be performed
         * @return this builder
         * @throws IllegalArgumentException if the tile height is negative
         */
        public Builder setTileHeight(int tileHeight) {
            if (tileHeight < 0) {
                throw new IllegalArgumentException(String.format("The supplied tile height (%d) is less than 0", tileHeight));
            }

            this.tileHeight = tileHeight;
            return this;
        }

        /**
         * @param padding  the padding (in pixels) of each tile at which the detection should be performed
         * @return this builder
         * @throws IllegalArgumentException if the padding is negative
         */
        public Builder setPadding(int padding) {
            if (padding < 0) {
                throw new IllegalArgumentException(String.format("The supplied padding (%d) is less than 0", padding));
            }

            this.padding = padding;
            return this;
        }

        /**
         * Objects created on the boundaries of tiles are merged with a shared boundary IoU criterion.
         * The value passed to this function is the minimum intersection-over-union (IoU) proportion
         * of the possibly-clipped boundary for merging (see {@link qupath.lib.objects.utils.ObjectMerger#createSharedTileBoundaryMerger(double)}
         *
         * @param boundaryThreshold  the minimum intersection-over-union (IoU) proportion of the possibly-clipped boundary for merging
         *                           (between 0 and 1)
         * @return this builder
         * @throws IllegalArgumentException if the threshold is not between 0 and 1
         */
        public Builder setBoundaryThreshold(float boundaryThreshold) {
            if (boundaryThreshold < 0 || boundaryThreshold > 1) {
                throw new IllegalArgumentException(String.format("The supplied boundary threshold (%f) is not within the required range ([0, 1])", boundaryThreshold));
            }

            this.boundaryThreshold = boundaryThreshold;
            return this;
        }

        /**
         * Set an operation to be run after the detection is complete.
         * This may be executed on any thread.
         *
         * @param onFinished  the operation to run after the detection
         * @return this builder
         */
        public Builder setOnFinished(Runnable onFinished) {
            this.onFinished = onFinished;
            return this;
        }

        /**
         * Build the {@link FatGlobulesDetectorParameters} instance.
         *
         * @return a new instance of {@link FatGlobulesDetectorParameters} with the defined parameters
         */
        public FatGlobulesDetectorParameters build() {
            return new FatGlobulesDetectorParameters(this);
        }
    }
}
