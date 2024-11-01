package com.shade.platform.model.util;

public final class MathUtils {
    public static final double HALF_PI = Math.PI / 2.0;

    private MathUtils() {
        // prevents instantiation
    }

    public static int alignUp(int value, int alignment) {
        return Math.ceilDiv(value, alignment) * alignment;
    }

    public static int wrapAround(int value, int max) {
        return (value % max + max) % max;
    }

    public static int wrapAround(int value, int min, int max) {
        if (value < min) {
            return max - (min - value) % (max - min);
        } else {
            return min + (value - min) % (max - min);
        }
    }
}
