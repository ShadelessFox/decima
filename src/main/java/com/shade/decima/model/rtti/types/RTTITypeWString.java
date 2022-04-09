package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;

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
        if (size > 0) {
            return new String(IOUtils.getBytesExact(buffer, size), StandardCharsets.UTF_16);
        } else {
            return "";
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_16);
        buffer.putInt(data.length / 2);
        buffer.put(data);
    }
}
