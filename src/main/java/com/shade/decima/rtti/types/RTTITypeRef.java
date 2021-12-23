package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIGenericType;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public class RTTITypeRef<T> implements RTTIGenericType<RTTIReference, T> {
    private final RTTIType<T> type;

    public RTTITypeRef(@NotNull RTTIType<T> type) {
        this.type = type;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull ByteBuffer buffer) {
        final RTTIReference.Type type = RTTIReference.Type.values()[buffer.get()];
        final UUID uuid = switch (type) {
            case INTERNAL_LINK, EXTERNAL_LINK, STREAMING_REFERENCE, UUID_REFERENCE -> RTTITypeRegistry.<UUID>find("GGUUID").read(buffer);
            default -> null;
        };
        final String path = switch (type) {
            case EXTERNAL_LINK, STREAMING_REFERENCE -> RTTITypeRegistry.<String>find("String").read(buffer);
            default -> null;
        };
        return new RTTIReference(type, uuid, path);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        buffer.put((byte) value.getType().ordinal());
        switch (value.getType()) {
            case INTERNAL_LINK, EXTERNAL_LINK, STREAMING_REFERENCE, UUID_REFERENCE -> RTTITypeRegistry.<UUID>find("GGUUID").write(buffer, Objects.requireNonNull(value.getUuid()));
            default -> {
            }
        }
        switch (value.getType()) {
            case INTERNAL_LINK, EXTERNAL_LINK -> RTTITypeRegistry.<String>find("String").write(buffer, Objects.requireNonNull(value.getPath()));
            default -> {
            }
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "Ref";
    }

    @NotNull
    @Override
    public Class<RTTIReference> getType() {
        return RTTIReference.class;
    }

    @Override
    public int getSize() {
        throw new IllegalStateException("getSize() is not implemented for dynamic containers");
    }

    @NotNull
    @Override
    public RTTIType<T> getUnderlyingType() {
        return type;
    }
}
