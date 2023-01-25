package com.shade.decima.ui.data.viewer.model.model.utils;


public class MathUtils {
    public static float toFloat(short h) {
        final int bits = h & 0xffff;
        final int sign = bits & 0x8000;
        final int exponent = (bits >>> 10) & 0x1f;
        final int mantissa = (bits) & 0x3ff;

        int outE = 0;
        int outM = 0;

        if (exponent == 0) {
            if (mantissa != 0) {
                final float o = Float.intBitsToFloat((126 << 23) + mantissa) - Float.intBitsToFloat(126 << 23);
                return sign == 0 ? o : -o;
            }
        } else {
            outM = mantissa << 13;
            if (exponent == 0x1f) {
                outE = 0xff;
                if (outM != 0) {
                    outM |= 0x400000;
                }
            } else {
                outE = exponent - 15 + 127;
            }
        }

        return Float.intBitsToFloat((sign << 16) | (outE << 23) | outM);
    }

    public static double dotProduct(double[] vec1, double[] vec2) {
        double res = 0;
        if (vec1.length != vec2.length) throw new RuntimeException("Vec1 length does not match vec2 length");
        for (int i = 0; i < vec1.length; i++) {
            res += vec1[i] * vec2[i];
        }
        return res;
    }
}
