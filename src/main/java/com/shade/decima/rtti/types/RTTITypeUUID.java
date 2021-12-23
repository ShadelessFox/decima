package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

@RTTIDefinition(name = "GGUUID")
public class RTTITypeUUID implements RTTIType<UUID> {
    @NotNull
    @Override
    public UUID read(@NotNull ByteBuffer buffer) {
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull UUID value) {
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
    }

    @NotNull
    @Override
    public Class<UUID> getType() {
        return UUID.class;
    }
}
