package com.shade.decima.model.rtti.types;

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
    public RTTIReference instantiate() {
        return RTTIReference.NONE;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final RTTIType<RTTIObject> GGUUID = registry.find("GGUUID");
        final RTTIType<String> String = registry.find("String");

        return switch (buffer.get()) {
            case 0 -> RTTIReference.NONE;
            case 1 -> new RTTIReference.Internal(RTTIReference.Kind.LINK, GGUUID.read(registry, buffer));
            case 2 -> new RTTIReference.External(RTTIReference.Kind.LINK, GGUUID.read(registry, buffer), String.read(registry, buffer));
            case 5 -> new RTTIReference.Internal(RTTIReference.Kind.REFERENCE, GGUUID.read(registry, buffer));
            case 3 -> new RTTIReference.External(RTTIReference.Kind.REFERENCE, GGUUID.read(registry, buffer), String.read(registry, buffer));
            default -> throw new IllegalArgumentException("Unsupported reference type");
        };
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        final RTTIType<RTTIObject> GGUUID = registry.find("GGUUID");
        final RTTIType<String> String = registry.find("String");

        if (value instanceof RTTIReference.External ref) {
            buffer.put((byte) (ref.kind() == RTTIReference.Kind.LINK ? 2 : 3));
            GGUUID.write(registry, buffer, ref.uuid());
            String.write(registry, buffer, ref.path());
        } else if (value instanceof RTTIReference.Internal ref) {
            buffer.put((byte) (ref.kind() == RTTIReference.Kind.LINK ? 1 : 5));
            GGUUID.write(registry, buffer, ref.uuid());
        } else {
            buffer.put((byte) 0);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIReference value) {
        final RTTIType<RTTIObject> GGUUID = registry.find("GGUUID");
        final RTTIType<String> String = registry.find("String");

        if (value instanceof RTTIReference.External ref) {
            return Byte.BYTES + GGUUID.getSize(registry, ref.uuid()) + String.getSize(registry, ref.path());
        } else if (value instanceof RTTIReference.Internal ref) {
            return Byte.BYTES + GGUUID.getSize(registry, ref.uuid());
        } else {
            return Byte.BYTES;
        }
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public RTTITypeParameterized<RTTIReference, ?> clone(@NotNull RTTIType<?> componentType) {
        if (type.equals(componentType)) {
            return this;
        } else {
            return new RTTITypeReference<>(name, componentType);
        }
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
