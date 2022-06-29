package com.shade.decima.model.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IOUtilsTest {
    @Test
    public void formatSizeTest() {
        assertEquals("0 B", IOUtils.formatSize(0));
        assertEquals("512 B", IOUtils.formatSize(512));
        assertEquals("1 kB", IOUtils.formatSize(1024));
        assertEquals("1.5 kB", IOUtils.formatSize(1536));
        assertEquals("6.21 mB", IOUtils.formatSize(6511657));
        assertEquals("2 gB", IOUtils.formatSize(Integer.MAX_VALUE));
        assertEquals("8 eB", IOUtils.formatSize(Long.MAX_VALUE));
    }
}
