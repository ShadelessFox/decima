package com.shade.decima.hrzr.rtti;

import com.shade.decima.model.util.hash.CRC32C;
import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.runtime.*;
import com.shade.decima.rtti.serde.ExtraBinaryDataHolder;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.GGUUID;

public class CoreFileReader implements Closeable {
    private final BinaryReader reader;
    private final TypeFactory factory;

    public CoreFileReader(@NotNull BinaryReader reader, @NotNull TypeFactory factory) {
        this.reader = reader;
        this.factory = factory;
    }

    @NotNull
    public List<Object> read() throws IOException {
        List<Object> objects = new ArrayList<>();

        while (reader.remaining() > 0) {
            var hash = reader.readLong();
            var size = reader.readInt();

            var start = reader.position();
            var object = readCompound(factory.get(HRZRTypeId.of(hash)));
            var end = reader.position();

            if (end - start != size) {
                throw new IllegalStateException("Size mismatch for " + factory.get(HRZRTypeId.of(hash)).name() + ": " + (end - start) + " != " + size);
            }

            objects.add(object);
        }

        return objects;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Nullable
    private Object readType(@NotNull TypeInfo info) throws IOException {
        return switch (info) {
            case AtomTypeInfo t -> readAtom(t);
            case EnumTypeInfo t -> readEnum(t);
            case ClassTypeInfo t -> readCompound(t);
            case ContainerTypeInfo t -> readContainer(t);
            case PointerTypeInfo t -> readPointer(t);
        };
    }

    @NotNull
    private Object readAtom(@NotNull AtomTypeInfo info) throws IOException {
        return switch (info.name().name()) {
            // Simple types
            case "uint8", "int8" -> reader.readByte();
            case "uint16", "int16" -> reader.readShort();
            case "uint", "int", "uint32", "int32" -> reader.readInt();
            case "uint64", "int64" -> reader.readLong();
            case "float" -> reader.readFloat();
            case "double" -> reader.readDouble();

            // Dynamic types
            case "String" -> readString(reader);
            case "WString" -> readWString(reader);

            // Aliases

            default -> throw new IllegalArgumentException("Unknown atom type: " + info.name());
        };
    }

    @NotNull
    private Object readEnum(@NotNull EnumTypeInfo info) throws IOException {
        int value = switch (info.size()) {
            case Byte.BYTES -> reader.readByte();
            case Short.BYTES -> reader.readShort();
            case Integer.BYTES -> reader.readInt();
            default -> throw new IllegalArgumentException("Unexpected enum size: " + info.size());
        };
        if (info.flags()) {
            return Value.setOf(uncheckedCast(info.type()), value);
        } else {
            return Value.valueOf(uncheckedCast(info.type()), value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

    @NotNull
    private Object readCompound(@NotNull ClassTypeInfo info) throws IOException {
        Object object = info.newInstance();
        for (ClassAttrInfo attr : info.serializableAttrs()) {
            attr.set(object, readType(attr.type().get()));
        }
        if (object instanceof ExtraBinaryDataHolder holder) {
            holder.deserialize(reader, factory);
        }
        return object;
    }

    @NotNull
    private Object readContainer(@NotNull ContainerTypeInfo info) throws IOException {
        var itemInfo = info.itemType().get();
        var itemType = itemInfo.type();
        var count = reader.readInt();

        // Fast path
        if (itemType == byte.class) {
            return reader.readBytes(count);
        } else if (itemType == short.class) {
            return reader.readShorts(count);
        } else if (itemType == int.class) {
            return reader.readInts(count);
        } else if (itemType == long.class) {
            return reader.readLongs(count);
        }

        // Slow path
        var array = Array.newInstance((Class<?>) itemType, count);
        for (int i = 0; i < count; i++) {
            Array.set(array, i, readType(itemInfo));
        }

        if (info.type() == List.class) {
            return Arrays.asList((Object[]) array);
        } else {
            return array;
        }
    }

    @Nullable
    private Ref<?> readPointer(@NotNull PointerTypeInfo info) throws IOException {
        var type = reader.readByte();
        var gguuid = factory.get(GGUUID.class);
        return switch (type) {
            case 0 -> null;
            case 1 -> new InternalLink<>((GGUUID) readCompound(gguuid));
            case 2 -> new ExternalLink<>((GGUUID) readCompound(gguuid), readString(reader));
            case 3 -> new StreamingRef<>((GGUUID) readCompound(gguuid), readString(reader));
            case 5 -> new UUIDRef<>((GGUUID) readCompound(gguuid));
            default -> throw new IllegalArgumentException("Unknown pointer type: " + type);
        };
    }

    @NotNull
    private static String readString(@NotNull BinaryReader reader) throws IOException {
        var length = reader.readInt();
        if (length == 0) {
            return "";
        }
        var hash = reader.readInt();
        var data = reader.readBytes(length);
        if (hash != CRC32C.calculate(data)) {
            throw new IllegalArgumentException("String is corrupted - mismatched checksum");
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    @NotNull
    private static String readWString(@NotNull BinaryReader reader) throws IOException {
        var length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length * 2, StandardCharsets.UTF_16LE);
    }

    private static final class InternalLink<T> implements Ref<T> {
        private final GGUUID objectUUID;
        private Object object;

        private InternalLink(@NotNull GGUUID objectUUID) {
            this.objectUUID = objectUUID;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) Objects.requireNonNull(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (InternalLink<?>) obj;
            return Objects.equals(obj, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public String toString() {
            return "<pointer to object " + objectUUID + ">";
        }
    }

    private record ExternalLink<T>(@NotNull GGUUID objectUUID, @NotNull String filename) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }

    private record StreamingRef<T>(@NotNull GGUUID objectUUID, @NotNull String filename) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }

    private record UUIDRef<T>(@NotNull GGUUID objectUUID) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }
}
