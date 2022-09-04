package com.shade.decima.viewer.mesh;

import com.shade.decima.ui.data.viewer.mesh.data.AccessorDataInt8;
import com.shade.decima.ui.data.viewer.mesh.data.ElementType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

public class AccessorDataTest {
    @Test
    public void accessorDataTest() {
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6});
        final AccessorDataInt8 accessor = new AccessorDataInt8(buffer, ElementType.VEC2, 3, 0, 0, 0, true, false);

        assertEquals(1, accessor.get(0, 0));
        assertEquals(2, accessor.get(0, 1));
        assertEquals(3, accessor.get(1, 0));
        assertEquals(4, accessor.get(1, 1));
        assertEquals(5, accessor.get(2, 0));
        assertEquals(6, accessor.get(2, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> accessor.get(2, 2));
    }
}
