package com.shade.decima.ui.data.viewer.shader.com;

import com.shade.util.NotNull;
import com.sun.jna.Structure;

import java.util.UUID;

@Structure.FieldOrder({"Data1", "Data2", "Data3", "Data4"})
public class GUID extends Structure {
    public int Data1;
    public short Data2;
    public short Data3;
    public byte[] Data4;

    public GUID(int data1, short data2, short data3, byte[] data4) {
        Data1 = data1;
        Data2 = data2;
        Data3 = data3;
        Data4 = data4;
    }

    public GUID(@NotNull String name) {
        final UUID uuid = UUID.fromString(name);

        Data1 = (int) (uuid.getMostSignificantBits() >>> 32);
        Data2 = (short) (uuid.getMostSignificantBits() >>> 16);
        Data3 = (short) (uuid.getMostSignificantBits());
        Data4 = new byte[]{
            (byte) (uuid.getLeastSignificantBits() >> 56),
            (byte) (uuid.getLeastSignificantBits() >> 48),
            (byte) (uuid.getLeastSignificantBits() >> 40),
            (byte) (uuid.getLeastSignificantBits() >> 32),
            (byte) (uuid.getLeastSignificantBits() >> 24),
            (byte) (uuid.getLeastSignificantBits() >> 16),
            (byte) (uuid.getLeastSignificantBits() >> 8),
            (byte) (uuid.getLeastSignificantBits()),
        };
    }
}
