package qupath.ext.liverquant.core;

/**
 * Define a pixel value in the HSV-space.
 *
 * @param hue  the hue of the pixel
 * @param saturation  the saturation of the pixel
 * @param value  the value of the pixel
 */
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
