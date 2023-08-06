package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RTTITypeEnum extends RTTIEnum {
    private final String name;
    private final Constant[] constants;
    private final int size;
    private final boolean flags;

    public RTTITypeEnum(@NotNull String name, @NotNull Constant[] constants, int size, boolean flags) {
        this.name = name;
        this.constants = constants;
        this.size = size;
        this.flags = flags;
    }

    @NotNull
    @Override
    public Constant[] getConstants() {
        return constants;
    }

    @NotNull
    @Override
    public Constant valueOf(int value) {
        if (isEnumSet()) {
            final Set<Constant> values = new HashSet<>();

            for (Constant constant : constants) {
                if ((value & constant.value()) != 0) {
                    values.add(constant);
                    value &= ~constant.value();
                }
            }

            if (value != 0) {
                throw new IllegalArgumentException("No constants found that match value " + value);
            }

            return new MyConstantSet(this, values);
        } else {
            for (Constant constant : constants) {
                if (value == constant.value()) {
                    return constant;
                }
            }

            throw new IllegalArgumentException("No constant found that matches value " + value);
        }
    }

    @Override
    public boolean isEnumSet() {
        return flags;
    }

    @NotNull
    @Override
    public Constant instantiate() {
        return constants[0];
    }

    @NotNull
    @Override
    public Constant copyOf(@NotNull Constant constant) {
        return constant;
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
            case 1 -> buffer.put((byte) constant.value());
            case 2 -> buffer.putShort((short) constant.value());
            case 4 -> buffer.putInt(constant.value());
            default -> throw new IllegalArgumentException("Unexpected enum size: " + size);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull Constant value) {
        return size;
    }

    @Override
    public int getSize() {
        return size;
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

    public record MyConstant(@NotNull RTTITypeEnum parent, @NotNull String name, int value) implements Constant {
        @Override
        public String toString() {
            return name;
        }
    }

    private record MyConstantSet(@NotNull RTTITypeEnum parent, @NotNull Set<Constant> values) implements ConstantSet {
        @NotNull
        @Override
        public String name() {
            return values.stream().map(Constant::name).collect(Collectors.joining("|"));
        }

        @Override
        public int value() {
            return values.stream().mapToInt(Constant::value).sum();
        }

        @NotNull
        @Override
        public Set<? extends Constant> constants() {
            return values;
        }

        @Override
        public String toString() {
            return name();
        }
    }
}
