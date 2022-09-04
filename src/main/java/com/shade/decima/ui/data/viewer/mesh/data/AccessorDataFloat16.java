package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AccessorDataFloat16 extends AccessorDataAbstract {
    public AccessorDataFloat16(@NotNull ByteBuffer buffer, @NotNull ElementType elementType, int elementCount, int componentCount, int stride, int offset) {
        super(buffer, elementType, ComponentType.UNSIGNED_SHORT, elementCount, componentCount, stride, offset);
    }

    public float get(int elementIndex, int componentIndex) {
        return toFloat(getBuffer().getShort(getPosition(elementIndex, componentIndex)));
    }

    public void put(int elementIndex, int componentIndex, float value) {
        throw new IllegalStateException("Not implemented");
    }

    private static float toFloat(short h) {
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
}
