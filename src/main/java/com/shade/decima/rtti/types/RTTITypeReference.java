package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeRegistry;
import com.shade.decima.rtti.objects.RTTIReference;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

@RTTIDefinition(name = "Ref", aliases = {"UUIDRef", "CPtr"})
public class RTTITypeReference<T> implements RTTIType<RTTIReference> {
    private final RTTIType<T> type;

    public RTTITypeReference(@NotNull RTTIType<T> type) {
        this.type = type;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull ByteBuffer buffer) {
        final RTTIReference.Type type = RTTIReference.Type.values()[buffer.get()];
        final UUID uuid = type.hasUuid() ? RTTITypeRegistry.<UUID>get("GGUUID").read(buffer) : null;
        final String path = type.hasPath() ? RTTITypeRegistry.<String>get("String").read(buffer) : null;
        return new RTTIReference(type, uuid, path);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        final RTTIReference.Type type = value.getType();
        buffer.put(type.getValue());
        if (type.hasUuid() && value.getUuid() != null) {
            RTTITypeRegistry.<UUID>get("GGUUID").write(buffer, value.getUuid());
        }
        if (type.hasPath() && value.getPath() != null) {
            RTTITypeRegistry.<String>get("String").write(buffer, value.getPath());
        }
    }

    @NotNull
    @Override
    public Class<RTTIReference> getType() {
        return RTTIReference.class;
    }

    @NotNull
    public RTTIType<T> getUnderlyingType() {
        return type;
    }
}
