package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.types.RTTITypeNumber;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RTTITypeNumberTest {
    private final RTTITypeNumber<BigInteger> type = new RTTITypeNumber<>("int128");

    @Test
    void roundTripsInt128Values() {
        for (BigInteger value : new BigInteger[]{
            BigInteger.ONE.shiftLeft(127).negate(),
            BigInteger.ONE.negate(),
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE)
        }) {
            final ByteBuffer buffer = ByteBuffer.allocate(16);
            type.write(null, buffer, value);
            assertEquals(value, type.read(null, buffer.flip()));
        }
    }

    @Test
    void rejectsValuesOutsideInt128Range() {
        assertThrows(IllegalArgumentException.class, () -> type.write(null, ByteBuffer.allocate(16), BigInteger.ONE.shiftLeft(127).negate().subtract(BigInteger.ONE)));
        assertThrows(IllegalArgumentException.class, () -> type.write(null, ByteBuffer.allocate(16), BigInteger.ONE.shiftLeft(127)));
    }
}
