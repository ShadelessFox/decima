package com.shade.decima.game.until_dawn.rtti;

import com.shade.decima.game.until_dawn.rtti.UntilDawn.EPlatform;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.RTTIRefObject;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.io.AbstractTypeReader;
import com.shade.decima.rtti.runtime.AtomTypeInfo;
import com.shade.decima.rtti.runtime.ContainerTypeInfo;
import com.shade.decima.rtti.runtime.EnumTypeInfo;
import com.shade.decima.rtti.runtime.PointerTypeInfo;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UntilDawnTypeReader extends AbstractTypeReader {
    private final List<Ref<?>> pointers = new ArrayList<>();
    private Header header;

    @NotNull
    public static <T> T readCompound(@NotNull Class<T> cls, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        return cls.cast(new UntilDawnTypeReader().readCompound(factory.get(cls), reader, factory));
    }

    @NotNull
    public List<RTTIRefObject> read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        header = Header.read(reader);

        var typeInfoCount = reader.readInt();
        var typeInfo = reader.readObjects(typeInfoCount, RTTITypeInfo::read);
        var objectTypesCount = reader.readInt();
        var objectTypes = reader.readInts(objectTypesCount);
        var totalExplicitObjects = reader.readInt();
        var objectHeaders = reader.readObjects(objectTypesCount, ObjectHeader::read);

        var objects = new ArrayList<RTTIRefObject>(header.assetCount);
        for (int i = 0; i < objectTypes.length; i++) {
            var start = reader.position();

            var info = typeInfo.get(objectTypes[i]);
            var header = objectHeaders.get(i);
            var object = (RTTIRefObject) readCompound(factory.get(UntilDawnTypeId.of(info.name)), reader, factory);

            var end = reader.position();
            if (header.size > 0 && end - start != header.size) {
                throw new IllegalStateException("Size mismatch for " + info.name + ": " + (end - start) + " != " + header.size);
            }

            objects.add(object);
        }

        resolvePointers(objects);

        return objects;
    }

    private void resolvePointers(@NotNull List<RTTIRefObject> objects) {
        for (Ref<?> pointer : pointers) {
            if (pointer instanceof LocalRef<?> localRef) {
                localRef.object = objects.get(localRef.index);
            }
        }

        pointers.clear();
    }

    @NotNull
    @Override
    protected Object readContainer(@NotNull ContainerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
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
        var array = Array.newInstance(itemType, count);
        for (int i = 0; i < count; i++) {
            Array.set(array, i, read(itemInfo, reader, factory));
        }

        if (info.type() == List.class) {
            return Arrays.asList((Object[]) array);
        } else {
            return array;
        }
    }

    @NotNull
    @Override
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
    @Override
    @SuppressWarnings("DuplicateBranchesInSwitch")
    protected Object readAtom(@NotNull AtomTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
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
    @Override
    protected Ref<?> readPointer(@NotNull PointerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
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

    @NotNull
    private String readString(@NotNull BinaryReader reader) throws IOException {
        int index = Objects.checkIndex(reader.readInt(), header.stringCount);
        if (index == 0) {
            return "";
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length);
    }

    @NotNull
    private String readWString(@NotNull BinaryReader reader) throws IOException {
        int index = Objects.checkIndex(reader.readInt(), header.wideStringCount);
        if (index == 0) {
            return "";
        }
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }
        return reader.readString(length * 2, StandardCharsets.UTF_16LE);
    }

    private record Header(
        int pointerMapSize,
        int stringCount,
        int wideStringCount,
        int assetCount
    ) {
        @NotNull
        static Header read(@NotNull BinaryReader reader) throws IOException {
            var version = reader.readString(14);
            if (!version.equals("RTTIBin<2.12> ")) {
                throw new IllegalStateException("Unsupported version: " + version);
            }
            var platform = EPlatform.valueOf(reader.readByte());
            if (platform != EPlatform.PINK) {
                throw new IllegalStateException("Unsupported platform: " + platform);
            }
            var endian = reader.readByte();
            if (endian != 0) {
                throw new IllegalStateException("Unsupported endian: " + endian);
            }
            var pointerMapSize = reader.readInt();
            var stringCount = reader.readInt();
            var wideStringCount = reader.readInt();
            var assetCount = reader.readInt();
            return new Header(pointerMapSize, stringCount, wideStringCount, assetCount);
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
            return obj instanceof LocalRef<?> that && index == that.index && object == that.object;
        }

        @Override
        public int hashCode() {
            return index;
        }

        @Override
        public String toString() {
            return "<pointer to object at " + index + ">";
        }
    }
}
