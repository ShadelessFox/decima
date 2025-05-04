package com.shade.decima.rtti.io;

import com.shade.decima.rtti.data.ExtraBinaryDataHolder;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.runtime.*;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public abstract class AbstractTypeReader {
    @Nullable
    public Object read(@NotNull TypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        return switch (info) {
            case AtomTypeInfo t -> readAtom(t, reader, factory);
            case EnumTypeInfo t -> readEnum(t, reader, factory);
            case ClassTypeInfo t -> readCompound(t, reader, factory);
            case ContainerTypeInfo t -> readContainer(t, reader, factory);
            case PointerTypeInfo t -> readPointer(t, reader, factory);
        };
    }

    @NotNull
    protected Object readCompound(@NotNull ClassTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Object object = info.newInstance();
        for (ClassAttrInfo attr : info.serializableAttrs()) {
            attr.set(object, read(attr.type().get(), reader, factory));
        }
        if (object instanceof ExtraBinaryDataHolder holder) {
            holder.deserialize(reader, factory);
        }
        return object;
    }

    @NotNull
    protected abstract Object readAtom(@NotNull AtomTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException;

    @NotNull
    protected Value<?> readEnum(@NotNull EnumTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        int value = switch (info.size()) {
            case Byte.BYTES -> reader.readByte();
            case Short.BYTES -> reader.readShort();
            case Integer.BYTES -> reader.readInt();
            default -> throw new IllegalArgumentException("Unexpected enum size: " + info.size());
        };
        if (info.isSet()) {
            return info.setOf(value);
        } else {
            return info.valueOf(value);
        }
    }

    @NotNull
    protected abstract Object readContainer(@NotNull ContainerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException;

    @Nullable
    protected abstract Ref<?> readPointer(@NotNull PointerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException;
}
