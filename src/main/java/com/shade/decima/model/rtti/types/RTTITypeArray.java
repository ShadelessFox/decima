package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Objects;

@RTTIDefinition({
    "Array",
    "EnvelopeSegment_MAX_ENVELOPE_SEGMENTS",
    "GlobalAppRenderVariableInfo_GLOBAL_APP_RENDER_VAR_COUNT",
    "GlobalRenderVariableInfo_GLOBAL_RENDER_VAR_COUNT",
    "ShaderProgramResourceSet_36",
    "float_GLOBAL_APP_RENDER_VAR_COUNT",
    "float_GLOBAL_RENDER_VAR_COUNT",
    "uint16_PBD_MAX_SKIN_WEIGHTS",
    "uint64_PLACEMENT_LAYER_MASK_SIZE",
    "uint8_PBD_MAX_SKIN_WEIGHTS"
})
public class RTTITypeArray<T> extends RTTITypeContainer<Object, T> {
    protected final String name;
    protected final RTTIType<T> type;

    public RTTITypeArray(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @NotNull
    public static Object read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIType<?> type, int length) {
        final Object array = Array.newInstance(type.getInstanceType(), length);

        for (int i = 0; i < length; i++) {
            Array.set(array, i, type.read(registry, buffer));
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T get(@NotNull Object array, int index) {
        return (T) Array.get(array, index);
    }

    public void set(@NotNull Object array, int index, @NotNull T value) {
        Array.set(array, index, value);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @NotNull
    public Object insert(@NotNull Object array, int index, @NotNull T value) {
        final int length = length(array);
        final Object result = Array.newInstance(type.getInstanceType(), length + 1);

        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index, result, index + 1, length - index);
        Array.set(result, index, value);

        return result;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @NotNull
    public Object remove(@NotNull Object array, int index) {
        final int length = length(array);
        final Object result = Array.newInstance(type.getInstanceType(), length - 1);

        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + 1, result, index, length - index - 1);

        return result;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @NotNull
    public Object move(@NotNull Object array, int src, int dst) {
        final int length = length(array);

        Objects.checkIndex(src, length);
        Objects.checkIndex(dst, length + 1);

        if (src != dst) {
            final Object value = Array.get(array, src);

            if (src < dst) {
                System.arraycopy(array, src + 1, array, src, dst - src);
            } else {
                System.arraycopy(array, dst, array, dst + 1, src - dst);
            }

            Array.set(array, dst, value);
        }

        return array;
    }

    @Override
    public int length(@NotNull Object array) {
        return Array.getLength(array);
    }

    @NotNull
    @Override
    public Object instantiate() {
        return Array.newInstance(type.getInstanceType(), 0);
    }

    @NotNull
    @Override
    public Object read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        return read(registry, buffer, type, buffer.getInt());
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull Object array) {
        final int length = length(array);

        buffer.putInt(length);

        for (int i = 0; i < length; i++) {
            type.write(registry, buffer, get(array, i));
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull Object array) {
        int size = Integer.BYTES;

        for (int i = 0, length = length(array); i < length; i++) {
            size += type.getSize(registry, get(array, i));
        }

        return size;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<Object> getInstanceType() {
        return (Class<Object>) type.getInstanceType().arrayType();
    }

    @NotNull
    @Override
    public RTTITypeParameterized<Object, ?> clone(@NotNull RTTIType<?> componentType) {
        if (type.equals(componentType)) {
            return this;
        } else {
            return new RTTITypeArray<>(name, componentType);
        }
    }

    @NotNull
    @Override
    public RTTIType<T> getComponentType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RTTITypeArray<?> that = (RTTITypeArray<?>) o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
