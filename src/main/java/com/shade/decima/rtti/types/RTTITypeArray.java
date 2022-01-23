package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIDefinition;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeContainer;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

@RTTIDefinition(name = "Array", aliases = {
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
public class RTTITypeArray<T> extends RTTITypeContainer<T[]> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeArray(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public T[] read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final T[] values = (T[]) Array.newInstance(type.getComponentType(), buffer.getInt());
        for (int i = 0; i < values.length; i++) {
            values[i] = type.read(registry, buffer);
        }
        return values;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull T[] values) {
        buffer.putInt(values.length);
        for (T value : values) {
            type.write(registry, buffer, value);
        }
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<T[]> getComponentType() {
        return (Class<T[]>) type.getComponentType().arrayType();
    }

    @NotNull
    @Override
    public RTTIType<?> getContainedType() {
        return type;
    }
}
