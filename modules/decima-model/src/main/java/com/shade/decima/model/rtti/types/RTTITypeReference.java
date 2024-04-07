package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

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
    public RTTIReference copyOf(@NotNull RTTIReference value) {
        return value;
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final byte type = buffer.get();
        return switch (type) {
            case 0 -> RTTIReference.NONE;
            case 1 -> {
                if (getTypeName().equals("UUIDRef")) {
                    yield new RTTIReference.Internal(RTTIReference.Kind.REFERENCE, factory.<RTTIType<RTTIObject>>find("GGUUID").read(factory, buffer));
                } else {
                    // ??? no idea how they're linked
                    yield RTTIReference.NONE;
                }
            }
            default -> throw new IllegalArgumentException("Unsupported reference type: " + type);
        };
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        final RTTIType<RTTIObject> GGUUID = factory.find("GGUUID");
        final RTTIType<String> String = factory.find("String");

        if (value instanceof RTTIReference.External ref) {
            buffer.put((byte) (ref.kind() == RTTIReference.Kind.LINK ? 2 : 3));
            GGUUID.write(factory, buffer, ref.uuid());
            String.write(factory, buffer, ref.path());
        } else if (value instanceof RTTIReference.Internal ref) {
            buffer.put((byte) (ref.kind() == RTTIReference.Kind.LINK ? 1 : 5));
            GGUUID.write(factory, buffer, ref.uuid());
        } else {
            buffer.put((byte) 0);
        }
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIReference value) {
        final RTTIType<RTTIObject> GGUUID = factory.find("GGUUID");
        final RTTIType<String> String = factory.find("String");

        if (value instanceof RTTIReference.External ref) {
            return Byte.BYTES + GGUUID.getSize(factory, ref.uuid()) + String.getSize(factory, ref.path());
        } else if (value instanceof RTTIReference.Internal ref) {
            return Byte.BYTES + GGUUID.getSize(factory, ref.uuid());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RTTITypeReference<?> that = (RTTITypeReference<?>) o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
