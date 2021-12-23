package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.CRC32C;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition(name = "String")
public class RTTITypeString implements RTTIType<String> {
    @NotNull
    @Override
    public String read(@NotNull ByteBuffer buffer) {
        final int length = buffer.getInt();
        if (length > 0) {
            final int checksum = buffer.getInt();
            final byte[] data = IOUtils.getBytesExact(buffer, length);
            if (checksum != (int) CRC32C.calculate(data)) {
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
            buffer.putInt((int) CRC32C.calculate(data));
            buffer.put(data);
        }
    }

    @NotNull
    @Override
    public Class<String> getType() {
        return String.class;
    }
}
