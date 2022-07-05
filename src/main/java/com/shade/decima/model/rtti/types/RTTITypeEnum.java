package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeEnum extends RTTIType<RTTITypeEnum.Constant> {
    private final String name;
    private final Constant[] constants;
    private final int size;

    public RTTITypeEnum(@NotNull String name, @NotNull Constant[] constants, int size) {
        this.name = name;
        this.constants = constants;
        this.size = size;
    }

    @NotNull
    @Override
    public Constant read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final int value = switch (size) {
            case 1 -> buffer.get() & 0xff;
            case 2 -> buffer.getShort() & 0xffff;
            case 4 -> buffer.getInt();
            default -> throw new IllegalArgumentException("Unexpected enum size: " + size);
        };

        return valueOf(value);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull Constant constant) {
        switch (size) {
            case 1 -> buffer.put((byte) constant.value);
            case 2 -> buffer.putShort((short) constant.value);
            case 4 -> buffer.putInt(constant.value);
            default -> throw new IllegalArgumentException("Unexpected enum size: " + size);
        }
    }

    @NotNull
    public Constant valueOf(int value) {
        for (Constant constant : constants) {
            if (constant.value == value) {
                return constant;
            }
        }

        throw new IllegalArgumentException("Enum " + getTypeName() + " has no constant with type " + value);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return name;
    }

    @NotNull
    @Override
    public Class<Constant> getInstanceType() {
        return Constant.class;
    }

    @NotNull
    public Constant[] getConstants() {
        return constants;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return getTypeName();
    }

    public static record Constant(@NotNull RTTITypeEnum parent, @NotNull String name, int value) {
        @Override
        public String toString() {
            return name;
        }
    }
}
