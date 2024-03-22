package com.shade.platform.model.util;

public class MathUtils {
    private MathUtils() {
        // prevents instantiation
    }

    public static int alignUp(int value, int alignment) {
        return (value + alignment - 1) / alignment * alignment;
    }

    public static int wrapAround(int index, int max) {
        return (index % max + max) % max;
    }

    // https://stackoverflow.com/a/6162687
    // TODO: Replace with Float#float16ToFloat once requires Java 21
    public static float halfToFloat(int value) {
        int mant = value & 0x03ff;
        int exp = value & 0x7c00;

        if (exp == 0x7c00) {
            exp = 0x3fc00;
        } else if (exp != 0) {
            exp += 0x1c000;
            if (mant == 0 && exp > 0x1c400) {
                return Float.intBitsToFloat((value & 0x8000) << 16 | exp << 13 | 0x3ff);
            }
        } else if (mant != 0) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while ((mant & 0x400) == 0);
            mant &= 0x3ff;
        }

        return Float.intBitsToFloat((value & 0x8000) << 16 | (exp | mant) << 13);
    }

    // https://stackoverflow.com/a/6162687
    // TODO: Replace with Float#floatToFloat16 once requires Java 21
    public static int floatToHalf(float value) {
        int bits = Float.floatToIntBits(value);
        int sign = bits >>> 16 & 0x8000;
        int val = (bits & 0x7fffffff) + 0x1000;

        if (val >= 0x47800000) {
            if ((bits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000) {
                    return sign | 0x7c00;
                } else {
                    return sign | 0x7c00 | (bits & 0x007fffff) >>> 13;
                }
            } else {
                return sign | 0x7bff;
            }
        }

        if (val >= 0x38800000) {
            return sign | val - 0x38000000 >>> 13;
        } else if (val < 0x33000000) {
            return sign;
        } else {
            val = (bits & 0x7fffffff) >>> 23;
            return sign | ((bits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
        }
    }

    // TODO: Replace with Math#clamp once requires Java 21
    public static float clamp(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    // TODO: Replace with Math#clamp once requires Java 21
    public static int clamp(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    public static short signExtend(short value, int bits) {
        final int shift = 32 - bits;
        return (short) (value << shift >> shift);
    }

    public static int signExtend(int value, int bits) {
        final int shift = 32 - bits;
        return value << shift >> shift;
    }
}
