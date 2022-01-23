package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class RTTITypeUUID implements RTTIType<UUID> {
    private final String name;

    public RTTITypeUUID(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public UUID read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull UUID value) {
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @NotNull
    @Override
    public Class<UUID> getComponentType() {
        return UUID.class;
    }
}
