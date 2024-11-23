package com.shade.decima.rtti.test;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.UntilDawn;
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

public class UntilDawnReader implements Closeable {
    private final BinaryReader reader;
    private final TypeFactory factory;

    private final Header header;
    private final RTTITypeInfo[] typeInfo;
    private final int[] objectTypes;
    private final ObjectHeader[] objectHeaders;

    private final List<Ref<?>> pointers = new ArrayList<>();

    public UntilDawnReader(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        this.reader = reader;
        this.header = Header.read(reader);
        this.factory = factory;

        var typeInfoCount = reader.readInt();
        this.typeInfo = reader.readObjects(typeInfoCount, RTTITypeInfo::read, RTTITypeInfo[]::new);

        var objectTypesCount = reader.readInt();
        this.objectTypes = reader.readInts(objectTypesCount);

        var totalExplicitObjects = reader.readInt();
        this.objectHeaders = reader.readObjects(objectTypesCount, ObjectHeader::read, ObjectHeader[]::new);
    }

    @NotNull
    public List<Object> read() throws IOException {
        List<Object> objects = new ArrayList<>(header.assetCount);

        for (int i = 0; i < objectTypes.length; i++) {
            var start = reader.position();

            var info = typeInfo[objectTypes[i]];
            var header = objectHeaders[i];
            var object = readCompound(factory.get(UntilDawnTypeId.of(info.name)));

            var end = reader.position();
            if (header.size > 0 && end - start != header.size) {
                throw new IllegalStateException("Size mismatch for " + info.name + ": " + (end - start) + " != " + header.size);
            }

            objects.add(object);
        }

        for (Ref<?> pointer : pointers) {
            if (pointer instanceof LocalRef<?> localRef) {
                localRef.object = objects.get(localRef.index);
            }
        }

        pointers.clear();

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

    @Nullable
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private Object readAtom(@NotNull AtomTypeInfo info) throws IOException {
        return switch (info.name().name()) {
            // Base types
            case "bool" -> reader.readByteBoolean();
            case "wchar" -> (char) reader.readShort();
            case "uint8", "int8" -> reader.readByte();
            case "uint16", "int16" -> reader.readShort();
            case "uint", "int", "uint32", "int32" -> reader.readInt();
            case "uint64", "int64" -> reader.readLong();
            case "float" -> reader.readFloat();
            case "double" -> reader.readDouble();
            case "String" -> readString(reader);
            case "WString" -> readWString(reader);

            // Aliases
            case "RenderDataPriority", "MaterialType" -> reader.readShort();
            case "PhysicsCollisionFilterInfo" -> reader.readInt();
            case "Filename" -> readString(reader);

            default -> throw new IllegalArgumentException("Unknown atom type: " + info.name());
        };
    }

    @Nullable
    private Ref<?> readPointer(@NotNull PointerTypeInfo info) throws IOException {
        var kind = reader.readByte();
        var pointer = switch (kind) {
            case 0 -> {
                int index = reader.readInt();
                if (index == 0) {
                    yield null;
                } else {
                    yield new LocalRef<>(index - 1);
                }
            }
            case 1 -> throw new IllegalStateException("External links are not supported");
            case 2 -> throw new IllegalStateException("Dependent links are not supported");
            default -> throw new NotImplementedException();
        };
        pointers.add(pointer);
        return pointer;
    }

    @Nullable
    private static String readString(@NotNull BinaryReader reader) throws IOException {
        int index = reader.readInt();
        if (index == 0) {
            return null;
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length);
    }

    @Nullable
    private static String readWString(@NotNull BinaryReader reader) throws IOException {
        int index = reader.readInt();
        if (index == 0) {
            return null;
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length * 2, StandardCharsets.UTF_16LE);
    }

    private record Header(
        int pointerMapSize,
        int allocationCount,
        int vramAllocationCount,
        int assetCount
    ) {
        @NotNull
        static Header read(@NotNull BinaryReader reader) throws IOException {
            var version = reader.readString(14);
            if (!version.equals("RTTIBin<2.12> ")) {
                throw new IllegalStateException("Unsupported version: " + version);
            }
            var platform = UntilDawn.EPlatform.valueOf(reader.readByte());
            if (platform != UntilDawn.EPlatform._3) {
                throw new IllegalStateException("Unsupported platform: " + platform);
            }
            var endian = reader.readByte();
            if (endian != 0) {
                throw new IllegalStateException("Unsupported endian: " + endian);
            }
            var pointerMapSize = reader.readInt();
            var allocationCount = reader.readInt();
            var vramAllocationCount = reader.readInt();
            var assetCount = reader.readInt();
            return new Header(pointerMapSize, allocationCount, vramAllocationCount, assetCount);
        }
    }

    private record RTTITypeInfo(@NotNull String name, @NotNull byte[] hash) {
        @NotNull
        static RTTITypeInfo read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var hash = reader.readBytes(16);
            return new RTTITypeInfo(name, hash);
        }
    }

    private record ObjectHeader(@NotNull byte[] hash, int size) {
        @NotNull
        static ObjectHeader read(@NotNull BinaryReader reader) throws IOException {
            var hash = reader.readBytes(16);
            var size = reader.readInt();
            return new ObjectHeader(hash, size);
        }
    }

    private static final class LocalRef<T> implements Ref<T> {
        private final int index;
        private Object object;

        private LocalRef(int index) {
            this.index = index;
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
            var that = (LocalRef<?>) obj;
            return Objects.equals(obj, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public String toString() {
            return "<pointer to object at " + index + ">";
        }
    }
}
