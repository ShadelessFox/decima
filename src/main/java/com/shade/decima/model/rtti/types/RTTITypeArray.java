package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.RTTIUtils;

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
public class RTTITypeArray<T> extends RTTITypeContainer<T[], T> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeArray(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @NotNull
    @Override
    public T[] read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        return RTTIUtils.readCollection(registry, buffer, type, buffer.getInt());
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
    public String getTypeName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<T[]> getInstanceType() {
        return (Class<T[]>) type.getInstanceType().arrayType();
    }

    @NotNull
    @Override
    public RTTIType<T> getArgumentType() {
        return type;
    }
}
