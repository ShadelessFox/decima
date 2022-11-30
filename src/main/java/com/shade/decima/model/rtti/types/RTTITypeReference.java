package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIDefinition({"Ref", "cptr", "StreamingRef", "UUIDRef", "WeakPtr"})
public class RTTITypeReference<T> extends RTTITypeParameterized<RTTIReference, T> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeReference(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final RTTIReference.Type type = RTTIReference.Type.valueOf(buffer.get());
        final RTTIObject uuid = type.hasUuid() ? (RTTIObject) registry.find("GGUUID").read(registry, buffer) : null;
        final String path = type.hasPath() ? (String) registry.find("String").read(registry, buffer) : null;
        return new RTTIReference(type, uuid, path);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        buffer.put(value.type().getValue());
        if (value.uuid() != null) {
            ((RTTIClass) registry.find("GGUUID")).write(registry, buffer, value.uuid());
        }
        if (value.path() != null) {
            ((RTTITypeString) registry.find("String")).write(registry, buffer, value.path());
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIReference value) {
        int size = Byte.BYTES;
        if (value.uuid() != null) {
            size += ((RTTIClass) registry.find("GGUUID")).getSize(registry, value.uuid());
        }
        if (value.path() != null) {
            size += ((RTTITypeString) registry.find("String")).getSize(registry, value.path());
        }
        return size;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public RTTIType<T> getComponentType() {
        return type;
    }

    @NotNull
    @Override
    public Class<RTTIReference> getInstanceType() {
        return RTTIReference.class;
    }
}
