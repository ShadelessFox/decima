package com.shade.platform.util;

import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IOUtilsTest {
    @Test
    public void formatSizeTest() {
        Assertions.assertEquals("0 B", IOUtils.formatSize(0));
        assertEquals("512 B", IOUtils.formatSize(512));
        assertEquals("1 kB", IOUtils.formatSize(1024));
        assertEquals("1.5 kB", IOUtils.formatSize(1536));
        assertEquals("6.21 mB", IOUtils.formatSize(6511657));
        assertEquals("2 gB", IOUtils.formatSize(Integer.MAX_VALUE));
        assertEquals("8 eB", IOUtils.formatSize(Long.MAX_VALUE));
    }

    @Test
    public void toHexDigitsTest() {
        assertEquals("00", IOUtils.toHexDigits((byte) 0x00));
        assertEquals("7F", IOUtils.toHexDigits((byte) 0x7F));
        assertEquals("10203040", IOUtils.toHexDigits(0x10203040, ByteOrder.BIG_ENDIAN));
        assertEquals("40302010", IOUtils.toHexDigits(0x10203040, ByteOrder.LITTLE_ENDIAN));
        assertEquals("1020304050607080", IOUtils.toHexDigits(0x1020304050607080L, ByteOrder.BIG_ENDIAN));
        assertEquals("8070605040302010", IOUtils.toHexDigits(0x1020304050607080L, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void signExtendTest() {
        assertEquals((short) 0, MathUtils.signExtend((short) 0, 10));
        assertEquals((short) -1, MathUtils.signExtend((short) 1023, 10));
        assertEquals((short) 511, MathUtils.signExtend((short) 511, 10));
        assertEquals((short) -512, MathUtils.signExtend((short) 512, 10));
        assertEquals(0, MathUtils.signExtend(0, 10));
        assertEquals(-1, MathUtils.signExtend(1023, 10));
        assertEquals(511, MathUtils.signExtend(511, 10));
        assertEquals(-512, MathUtils.signExtend(512, 10));
    }
}
