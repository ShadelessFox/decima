package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.CRC32C;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition(name = "WString")
public class RTTITypeWString extends RTTITypeString {
    public RTTITypeWString(@NotNull String name) {
        super(name);
    }

    @NotNull
    @Override
    public String read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final int size = buffer.getInt() * 2;
        final int hash = buffer.getInt();
        return new String(IOUtils.getBytesExact(buffer, size, hash), StandardCharsets.UTF_16);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_16);
        buffer.putInt(data.length / 2);
        buffer.putInt(CRC32C.calculate(data));
        buffer.put(data);
    }
}
