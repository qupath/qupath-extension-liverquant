package qupath.ext.liverquant.core;

import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;

import java.util.Collection;

public class FatGlobulesDetectorParameters {

    private final ImageData<?> imageData;
    private final Collection<PathObject> annotations;
    private final ObjectCreated objectCreated;
    private final float pixelSize;
    private final int lowerBoundHue;
    private final int lowerBoundSaturation;
    private final int lowerBoundValue;
    private final int upperBoundHue;
    private final int upperBoundSaturation;
    private final int upperBoundValue;
    private final float minFatGlobuleElongation;
    private final float minOverlappingFatGlobuleElongation;
    private final float minFatGlobuleSolidity;
    private final float minOverlappingFatGlobuleSolidity;
    private final float minDiameter;
    private final float maxDiameter;
    private final int tileWidth;
    private final int tileHeight;
    private final int padding;
    private final float boundaryThreshold;
    public enum ObjectCreated {
        ANNOTATION,
        DETECTION
    }

    private FatGlobulesDetectorParameters(Builder builder) {
        this.imageData = builder.imageData;
        this.annotations = builder.annotations;
        this.objectCreated = builder.objectCreated;
        this.pixelSize = builder.pixelSize;
        this.lowerBoundHue = builder.lowerBoundHue;
        this.lowerBoundSaturation = builder.lowerBoundSaturation;
        this.lowerBoundValue = builder.lowerBoundValue;
        this.upperBoundHue = builder.upperBoundHue;
        this.upperBoundSaturation = builder.upperBoundSaturation;
        this.upperBoundValue = builder.upperBoundValue;
        this.minFatGlobuleElongation = builder.minFatGlobuleElongation;
        this.minOverlappingFatGlobuleElongation = builder.minOverlappingFatGlobuleElongation;
        this.minFatGlobuleSolidity = builder.minFatGlobuleSolidity;
        this.minOverlappingFatGlobuleSolidity = builder.minOverlappingFatGlobuleSolidity;
        this.minDiameter = builder.minDiameter;
        this.maxDiameter = builder.maxDiameter;
        this.tileWidth = builder.tileWidth;
        this.tileHeight = builder.tileHeight;
        this.padding = builder.padding;
        this.boundaryThreshold = builder.boundaryThreshold;
    }

    public ImageData<?> getImageData() {
        return imageData;
    }

    public Collection<PathObject> getAnnotations() {
        return annotations;
    }

    public ObjectCreated getObjectCreated() {
        return objectCreated;
    }

    public float getPixelSize() {
        return pixelSize;
    }

    public int getLowerBoundHue() {
        return lowerBoundHue;
    }

    public int getLowerBoundSaturation() {
        return lowerBoundSaturation;
    }

    public int getLowerBoundValue() {
        return lowerBoundValue;
    }

    public int getUpperBoundHue() {
        return upperBoundHue;
    }

    public int getUpperBoundSaturation() {
        return upperBoundSaturation;
    }

    public int getUpperBoundValue() {
        return upperBoundValue;
    }

    public float getMinFatGlobuleElongation() {
        return minFatGlobuleElongation;
    }

    public float getMinOverlappingFatGlobuleElongation() {
        return minOverlappingFatGlobuleElongation;
    }

    public float getMinFatGlobuleSolidity() {
        return minFatGlobuleSolidity;
    }

    public float getMinOverlappingFatGlobuleSolidity() {
        return minOverlappingFatGlobuleSolidity;
    }

    public float getMinDiameter() {
        return minDiameter;
    }

    public float getMaxDiameter() {
        return maxDiameter;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getPadding() {
        return padding;
    }

    public float getBoundaryThreshold() {
        return boundaryThreshold;
    }

    public static class Builder {

        private final ImageData<?> imageData;
        private final Collection<PathObject> annotations;
        private ObjectCreated objectCreated = ObjectCreated.DETECTION;
        private float pixelSize = 0.5f;
        private int lowerBoundHue = 0;
        private int lowerBoundSaturation = 0;
        private int lowerBoundValue = 200;
        private int upperBoundHue = 180;
        private int upperBoundSaturation = 25;
        private int upperBoundValue = 255;
        private float minFatGlobuleElongation = 0.4f;
        private float minOverlappingFatGlobuleElongation = 0.05f;
        private float minFatGlobuleSolidity = 0.85f;
        private float minOverlappingFatGlobuleSolidity = 0.7f;
        private float minDiameter = 5;
        private float maxDiameter = 100;
        private int tileWidth = 512;
        private int tileHeight = 512;
        private int padding = 64;
        private float boundaryThreshold = 0.5f;

        public Builder(ImageData<?> imageData, Collection<PathObject> annotations) {
            this.imageData = imageData;
            this.annotations = annotations;
        }

        public void setObjectCreated(ObjectCreated objectCreated) {
            this.objectCreated = objectCreated;
        }

        public void setPixelSize(float pixelSize) {
            this.pixelSize = pixelSize;
        }

        public void setLowerBoundHue(int lowerBoundHue) {
            if (lowerBoundHue < 0 || lowerBoundHue > 179) {
                throw new IllegalArgumentException(String.format("The supplied hue (%d) is not within the required range ([0, 179])", lowerBoundHue));
            }

            this.lowerBoundHue = lowerBoundHue;
        }

        public void setLowerBoundSaturation(int lowerBoundSaturation) {
            if (lowerBoundSaturation < 0 || lowerBoundSaturation > 255) {
                throw new IllegalArgumentException(String.format("The supplied saturation (%d) is not within the required range ([0, 255])", lowerBoundSaturation));
            }

            this.lowerBoundSaturation = lowerBoundSaturation;
        }

        public void setLowerBoundValue(int lowerBoundValue) {
            if (lowerBoundValue < 0 || lowerBoundValue > 255) {
                throw new IllegalArgumentException(String.format("The supplied value (%d) is not within the required range ([0, 255])", lowerBoundValue));
            }

            this.lowerBoundValue = lowerBoundValue;
        }

        public void setUpperBoundHue(int upperBoundHue) {
            if (upperBoundHue < 0 || upperBoundHue > 179) {
                throw new IllegalArgumentException(String.format("The supplied hue (%d) is not within the required range ([0, 179])", upperBoundHue));
            }

            this.upperBoundHue = upperBoundHue;
        }

        public void setUpperBoundSaturation(int upperBoundSaturation) {
            if (upperBoundSaturation < 0 || upperBoundSaturation > 255) {
                throw new IllegalArgumentException(String.format("The supplied saturation (%d) is not within the required range ([0, 255])", upperBoundSaturation));
            }

            this.upperBoundSaturation = upperBoundSaturation;
        }

        public void setUpperBoundValue(int upperBoundValue) {
            if (upperBoundValue < 0 || upperBoundValue > 255) {
                throw new IllegalArgumentException(String.format("The supplied value (%d) is not within the required range ([0, 255])", upperBoundValue));
            }

            this.upperBoundValue = upperBoundValue;
        }

        public void setMinFatGlobuleElongation(float minFatGlobuleElongation) {
            this.minFatGlobuleElongation = minFatGlobuleElongation;
        }

        public void setMinOverlappingFatGlobuleElongation(float minOverlappingFatGlobuleElongation) {
            this.minOverlappingFatGlobuleElongation = minOverlappingFatGlobuleElongation;
        }

        public void setMinFatGlobuleSolidity(float minFatGlobuleSolidity) {
            this.minFatGlobuleSolidity = minFatGlobuleSolidity;
        }

        public void setMinOverlappingFatGlobuleSolidity(float minOverlappingFatGlobuleSolidity) {
            this.minOverlappingFatGlobuleSolidity = minOverlappingFatGlobuleSolidity;
        }

        public void setMinDiameter(float minDiameter) {
            this.minDiameter = minDiameter;
        }

        public void setMaxDiameter(float maxDiameter) {
            this.maxDiameter = maxDiameter;
        }

        public void setTileWidth(int tileWidth) {
            if (tileWidth < 0) {
                throw new IllegalArgumentException(String.format("The supplied tile width (%d) is less than 0", tileWidth));
            }

            this.tileWidth = tileWidth;
        }

        public void setTileHeight(int tileHeight) {
            if (tileHeight < 0) {
                throw new IllegalArgumentException(String.format("The supplied tile height (%d) is less than 0", tileHeight));
            }

            this.tileHeight = tileHeight;
        }

        public void setPadding(int padding) {
            if (padding < 0) {
                throw new IllegalArgumentException(String.format("The supplied padding (%d) is less than 0", padding));
            }

            this.padding = padding;
        }

        public void setBoundaryThreshold(float boundaryThreshold) {
            if (boundaryThreshold < 0 || boundaryThreshold > 0) {
                throw new IllegalArgumentException(String.format("The supplied boundary threshold (%f) is not within the required range ([0, 1])", boundaryThreshold));
            }

            this.boundaryThreshold = boundaryThreshold;
        }
    }
}
