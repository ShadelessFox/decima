package com.shade.decima.ui.data.viewer.shader.ffm;

import java.lang.foreign.MemorySegment;
import java.util.UUID;

import static java.lang.foreign.ValueLayout.*;

public record GUID(long msb, long lsb) {
    public static final int BYTES = 16;

    public static GUID of(String name) {
        UUID uuid = UUID.fromString(name);
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return new GUID(msb, lsb);
    }

    public void set(MemorySegment segment, int offset) {
        segment.set(JAVA_INT, offset, (int) (msb >>> 32));
        segment.set(JAVA_SHORT, offset + 4, (short) (msb >>> 16));
        segment.set(JAVA_SHORT, offset + 6, (short) msb);
        segment.set(JAVA_BYTE, offset + 8, (byte) (lsb >>> 56));
        segment.set(JAVA_BYTE, offset + 9, (byte) (lsb >>> 48));
        segment.set(JAVA_BYTE, offset + 10, (byte) (lsb >>> 40));
        segment.set(JAVA_BYTE, offset + 11, (byte) (lsb >>> 32));
        segment.set(JAVA_BYTE, offset + 12, (byte) (lsb >>> 24));
        segment.set(JAVA_BYTE, offset + 13, (byte) (lsb >>> 16));
        segment.set(JAVA_BYTE, offset + 14, (byte) (lsb >>> 8));
        segment.set(JAVA_BYTE, offset + 15, (byte) lsb);
    }
}
