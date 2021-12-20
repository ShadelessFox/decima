package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.CRC32C;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RTTITypeString implements RTTIType<String> {
    @NotNull
    @Override
    public String read(@NotNull ByteBuffer buffer) {
        final int length = buffer.getInt();
        if (length > 0) {
            final int checksum = buffer.getInt();
            final byte[] data = IOUtils.getBytesExact(buffer, length);
            if (checksum != getChecksum(data)) {
                throw new IllegalArgumentException("String data is corrupted (mismatched checksum)");
            }
            return new String(data, StandardCharsets.UTF_8);
        }
        return "";
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(data.length);
        if (data.length > 0) {
            buffer.putInt(getChecksum(data));
            buffer.put(data);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "String";
    }

    @NotNull
    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public int getSize() {
        throw new IllegalStateException("getSize() is not implemented for dynamic containers");
    }

    private static int getChecksum(@NotNull byte[] data) {
        final CRC32C crc = new CRC32C();
        crc.update(data);
        return (int) crc.getValue();
    }
}
