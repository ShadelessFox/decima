package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeContainer;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.objects.RTTIReference;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

@RTTIDefinition(name = "Ref", aliases = {"CPtr", "StreamingRef", "UUIDRef", "WeakPtr"})
public class RTTITypeReference<T> extends RTTITypeContainer<RTTIReference> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeReference(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull ByteBuffer buffer) {
        final RTTIReference.Type type = RTTIReference.Type.values()[buffer.get()];
        final RTTIObject uuid = type.hasUuid() ? (RTTIObject) RTTITypeRegistry.getInstance().find("GGUUID").read(buffer) : null;
        final String path = type.hasPath() ? (String) RTTITypeRegistry.getInstance().find("String").read(buffer) : null;
        return new RTTIReference(type, uuid, path);
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.REFERENCE;
    }

    @NotNull
    @Override
    public RTTIType<?> getContainedType() {
        return type;
    }

    @NotNull
    @Override
    public Class<RTTIReference> getComponentType() {
        return RTTIReference.class;
    }
}
