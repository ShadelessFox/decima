package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class RTTITypeEnumFlags extends RTTIType<Set<RTTITypeEnumFlags.Constant>> {
    private final String name;
    private final Constant[] constants;
    private final int size;

    public RTTITypeEnumFlags(@NotNull String name, @NotNull Constant[] constants, int size) {
        this.name = name;
        this.constants = constants;
        this.size = size;
    }

    @NotNull
    @Override
    public Set<Constant> instantiate() {
        return Set.of();
    }

    @NotNull
    @Override
    public Set<Constant> copyOf(@NotNull Set<Constant> constants) {
        return Set.copyOf(constants);
    }

    @NotNull
    @Override
    public Set<Constant> read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final int value = switch (size) {
            case 1 -> buffer.get() & 0xff;
            case 2 -> buffer.getShort() & 0xffff;
            case 4 -> buffer.getInt();
            default -> throw new IllegalArgumentException("Unexpected enum size: " + size);
        };

        final Set<Constant> constants = new HashSet<>();

        for (RTTITypeEnumFlags.Constant constant : this.constants) {
            if ((constant.value & value) > 0) {
                constants.add(constant);
            }
        }

        return constants;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull Set<Constant> constants) {
        int value = 0;

        for (Constant constant : constants) {
            value |= constant.value();
        }

        switch (size) {
            case 1 -> buffer.put((byte) value);
            case 2 -> buffer.putShort((short) value);
            case 4 -> buffer.putInt(value);
            default -> throw new IllegalArgumentException("Unexpected enum size: " + size);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull Set<Constant> value) {
        return size;
    }

    public int getSize() {
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
    public Class<Set<Constant>> getInstanceType() {
        return (Class<Set<Constant>>) (Object) Set.class;
    }

    @NotNull
    public Constant[] getConstants() {
        return constants;
    }

    @Override
    public String toString() {
        return getTypeName();
    }

    public record Constant(@NotNull RTTITypeEnumFlags parent, @NotNull String name, int value) {
        @Override
        public String toString() {
            return name;
        }
    }
}
