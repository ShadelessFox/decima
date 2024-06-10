package com.shade.platform.util;

import com.shade.platform.model.util.MathUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathUtilsTest {
    @Test
    public void wrapAroundTest() {
        assertEquals(1, MathUtils.wrapAround(1, 1, 3));
        assertEquals(2, MathUtils.wrapAround(2, 1, 3));
        assertEquals(1, MathUtils.wrapAround(3, 1, 3));
        assertEquals(9, MathUtils.wrapAround(4, 5, 10));
    }
}
