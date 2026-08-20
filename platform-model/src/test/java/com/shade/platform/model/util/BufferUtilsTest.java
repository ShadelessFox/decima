package com.shade.platform.model.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BufferUtilsTest {
    @Test
    void roundTripsUInt128Values() {
        for (BigInteger value : new BigInteger[]{
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.ONE.shiftLeft(127),
            BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)
        }) {
            final ByteBuffer buffer = ByteBuffer.allocate(16);
            BufferUtils.putUInt128(buffer, value);
            assertEquals(value, BufferUtils.getUInt128(buffer.flip()));
        }
    }

    @Test
    void rejectsValuesOutsideUInt128Range() {
        assertThrows(IllegalArgumentException.class, () -> BufferUtils.putUInt128(ByteBuffer.allocate(16), BigInteger.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> BufferUtils.putUInt128(ByteBuffer.allocate(16), BigInteger.ONE.shiftLeft(128)));
    }
}
