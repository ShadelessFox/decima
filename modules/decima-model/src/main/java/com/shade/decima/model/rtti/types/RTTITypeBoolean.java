package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIDefinition("bool")
public class RTTITypeBoolean extends RTTITypePrimitive<Boolean> {
    private final String name;

    public RTTITypeBoolean(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public Boolean instantiate() {
        return false;
    }

    @NotNull
    @Override
    public Boolean copyOf(@NotNull Boolean value) {
        return value;
    }

    @NotNull
    @Override
    public Boolean read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        return buffer.get() != 0;
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull Boolean value) {
        buffer.put(value ? (byte) 1 : 0);
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull Boolean value) {
        return Byte.BYTES;
    }

    @NotNull
    @Override
    public Class<Boolean> getInstanceType() {
        return Boolean.class;
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public RTTITypePrimitive<? super Boolean> clone(@NotNull String name) {
        return new RTTITypeBoolean(name);
    }
}
