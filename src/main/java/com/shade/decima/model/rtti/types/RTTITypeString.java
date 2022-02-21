package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.hash.CRC32C;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition(name = "String")
public class RTTITypeString implements RTTIType<String> {
    private final String name;

    public RTTITypeString(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final int size = buffer.getInt();
        if (size > 0) {
            final int hash = buffer.getInt();
            return new String(IOUtils.getBytesExact(buffer, size, hash), StandardCharsets.UTF_8);
        } else {
            return "";
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(data.length);
        if (data.length > 0) {
            buffer.putInt(CRC32C.calculate(data));
            buffer.put(data);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.PRIMITIVE;
    }

    @NotNull
    @Override
    public Class<String> getComponentType() {
        return String.class;
    }
}
