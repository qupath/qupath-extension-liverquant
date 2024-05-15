package qupath.ext.liverquant.core;

import qupath.lib.images.servers.ImageServer;

import java.awt.image.BufferedImage;

/**
 * <p>
 *     Parameters to start a {@link TissueDetector}.
 * </p>
 * <p>
 *     Use the {@link TissueDetectorParameters.Builder} to create an instance of this object.
 * </p>
 */
public class TissueDetectorParameters {

    private final ImageServer<BufferedImage> server;
    private final HsvArray lowerBound;
    private final HsvArray upperBound;
    private final double downsample;
    private final double minTissueArea;

    private TissueDetectorParameters(TissueDetectorParameters.Builder builder) {
        this.server = builder.server;
        this.lowerBound = builder.lowerBound;
        this.upperBound = builder.upperBound;
        this.downsample = builder.downsample;
        this.minTissueArea = builder.minTissueArea;
    }

    /**
     * @return the ImageServer representing the image to use the algorithm on
     */
    public ImageServer<BufferedImage> getServer() {
        return server;
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
     * @return the downsample to use on the image when performing the detection
     */
    public double getDownsample() {
        return downsample;
    }

    /**
     * @return the minimum area a tissue should have (in micro-meter squared)
     */
    public double getMinTissueArea() {
        return minTissueArea;
    }

    /**
     * Create an instance of {@link TissueDetectorParameters}.
     */
    public static class Builder {

        private final ImageServer<BufferedImage> server;
        private HsvArray lowerBound = new HsvArray(0, 0, 200);
        private HsvArray upperBound = new HsvArray(180, 10, 255);
        private double downsample = 32;
        private double minTissueArea = 5e5;

        /**
         * Create the builder.
         *
         * @param server  the ImageServer representing the image to use the algorithm on
         */
        public Builder(ImageServer<BufferedImage> server) {
            this.server = server;
        }

        /**
         * @param lowerBound  the inclusive lower bound array in HSV-space that should be used for color segmentation
         * @return this builder
         */
        public TissueDetectorParameters.Builder setLowerBound(HsvArray lowerBound) {
            this.lowerBound = lowerBound;
            return this;
        }

        /**
         * @param upperBound  the inclusive upper bound array in HSV-space that should be used for color segmentation
         * @return this builder
         */
        public TissueDetectorParameters.Builder setUpperBound(HsvArray upperBound) {
            this.upperBound = upperBound;
            return this;
        }

        /**
         * @param downsample  the downsample to use on the image when performing the detection
         * @return this builder
         */
        public TissueDetectorParameters.Builder setDownsample(double downsample) {
            this.downsample = downsample;
            return this;
        }

        /**
         * @param minTissueArea  the minimum area a tissue should have (in micro-meter squared)
         * @return this builder
         */
        public TissueDetectorParameters.Builder setMinTissueArea(double minTissueArea) {
            this.minTissueArea = minTissueArea;
            return this;
        }

        /**
         * Build the {@link TissueDetectorParameters} instance.
         *
         * @return a new instance of {@link TissueDetectorParameters} with the defined parameters
         */
        public TissueDetectorParameters build() {
            return new TissueDetectorParameters(this);
        }
    }
}
