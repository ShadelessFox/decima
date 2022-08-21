package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIDefinition(name = "Ref", aliases = {"cptr", "StreamingRef", "UUIDRef", "WeakPtr"})
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
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public RTTIType<T> getArgumentType() {
        return type;
    }

    @NotNull
    @Override
    public Class<RTTIReference> getInstanceType() {
        return RTTIReference.class;
    }
}
