package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeEnum<T extends Number> implements RTTIType<RTTITypeEnum.Constant<T>> {
    private final RTTIType<T> type;
    private final Constant<T>[] constants;

    public RTTITypeEnum(@NotNull RTTIType<T> type, @NotNull Constant<T>[] constants) {
        this.type = type;
        this.constants = constants;
    }

    @NotNull
    @Override
    public Constant<T> read(@NotNull ByteBuffer buffer) {
        final T value = type.read(buffer);

        for (Constant<T> constant : constants) {
            if (constant.value().equals(value)) {
                return constant;
            }
        }

        throw new IllegalArgumentException("Enum '" + this + "' does not have a constant with value '" + value + "'");
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Constant<T> value) {
        for (Constant<T> constant : constants) {
            if (constant.equals(value)) {
                type.write(buffer, constant.value());
                return;
            }
        }

        throw new IllegalArgumentException("Enum '" + this + "' does not have a constant called '" + value.name() + "' with value '" + value.value() + "'");
    }

    @NotNull
    public Constant<T>[] getConstants() {
        return constants;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<Constant<T>> getType() {
        return (Class<Constant<T>>) (Object) Constant.class;
    }

    @Override
    public String toString() {
        return RTTITypeRegistry.getInstance().getName(this);
    }

    public record Constant<T extends Number>(@NotNull RTTITypeEnum<T> parent, @NotNull String name, @NotNull T value) {
        @Override
        public String toString() {
            return name;
        }
    }
}
