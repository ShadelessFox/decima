package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import com.shade.util.hash.HashFunction;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RTTIDefinition("WString")
public class RTTITypeWString extends RTTITypeString {
    public RTTITypeWString(@NotNull String name) {
        super(name);
    }

    @NotNull
    @Override
    public String read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final int size = buffer.getInt() * 2;
        if (size > 0) {
            return new String(BufferUtils.getBytes(buffer, size), StandardCharsets.UTF_16LE);
        } else {
            return "";
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull String value) {
        final byte[] data = value.getBytes(StandardCharsets.UTF_16LE);
        buffer.putInt(data.length / 2);
        buffer.put(data);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull String value) {
        if (value.isEmpty()) {
            return Integer.BYTES;
        } else {
            return Integer.BYTES + value.getBytes(StandardCharsets.UTF_16LE).length;
        }
    }

    @NotNull
    @Override
    public RTTITypePrimitive<? super String> clone(@NotNull String name) {
        return new RTTITypeWString(name);
    }

    @Override
    public int getHash(@NotNull String value) {
        return HashFunction.crc32c().hash(value, StandardCharsets.UTF_16LE).asInt();
    }
}
