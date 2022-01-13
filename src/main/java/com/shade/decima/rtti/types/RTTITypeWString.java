package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.CRC32C;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition(name = "WString")
public final class RTTITypeWString implements RTTIType<String> {
    private final String name;

    public RTTITypeWString(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String read(@NotNull ByteBuffer buffer) {
        final int size = buffer.getInt() * 2;
        final int hash = buffer.getInt();
        return new String(IOUtils.getBytesExact(buffer, size, hash), StandardCharsets.UTF_16);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_16);
        buffer.putInt(data.length / 2);
        buffer.putInt(CRC32C.calculate(data));
        buffer.put(data);
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
