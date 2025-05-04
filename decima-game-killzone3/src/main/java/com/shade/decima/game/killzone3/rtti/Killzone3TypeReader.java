package com.shade.decima.game.killzone3.rtti;

import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.io.AbstractTypeReader;
import com.shade.decima.rtti.runtime.AtomTypeInfo;
import com.shade.decima.rtti.runtime.ContainerTypeInfo;
import com.shade.decima.rtti.runtime.PointerTypeInfo;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public final class Killzone3TypeReader extends AbstractTypeReader {
    private final List<Ref<?>> pointers = new ArrayList<>();
    private List<String> objectTypes;
    private Map<String, List<Object>> atomTables;

    public static <T> T readCompound(Class<T> cls, BinaryReader reader, TypeFactory factory) throws IOException {
        return cls.cast(new Killzone3TypeReader().readCompound(factory.get(cls), reader, factory));
    }

    public List<Object> read(BinaryReader reader, TypeFactory factory) throws IOException {
        var bin = Header.read(reader);
        var typeInfo = reader.readObjects(readVarInt(reader), Killzone3TypeReader::readVarString);
        var objectTypes = reader.readObjects(readVarInt(reader), r -> typeInfo.get(readSizeInt(r, typeInfo.size())));
        var totalExplicitObjects = readVarInt(reader);
        var objectHeaders = reader.readObjects(objectTypes.size(), ObjectHeader::read);
        var atomTableAllocations = reader.readObjects(readVarInt(reader), Allocation::read);
        var atomTables = reader.readObjects(readVarInt(reader), r -> AtomTable.read(r, typeInfo));
        var indirectObjectIndex = readVarInt(reader);
        var objectConstructionAllocations = reader.readObjects(readVarInt(reader), Allocation::read);

        this.objectTypes = objectTypes;
        this.atomTables = atomTables.stream().collect(Collectors.toMap(AtomTable::type, AtomTable::entries));

        var objects = new ArrayList<>(objectTypes.size());
        for (int i = 0; i < objectTypes.size(); i++) {
            var allocations = reader.readObjects(readVarInt(reader), Allocation::read);
            var vramAllocations = reader.readObjects(readVarInt(reader), Allocation::read);

            var start = reader.position();

            var type = factory.get(Killzone3TypeId.of(objectTypes.get(i)));
            var header = objectHeaders.get(i);
            var object = readCompound(type, reader, factory);

            var end = reader.position();
            if (header.size > 0 && end - start != header.size) {
                throw new IllegalStateException("Size mismatch for %s: %d (actual) != %d (expected)".formatted(type, end - start, header.size));
            }

            objects.add(object);
        }

        resolvePointers(objects);

        return objects;
    }

    private void resolvePointers(@NotNull List<Object> objects) {
        for (Ref<?> pointer : pointers) {
            if (pointer instanceof LocalRef<?> localRef) {
                localRef.object = objects.get(localRef.index);
            }
        }

        pointers.clear();
    }

    @Override
    @SuppressWarnings("DuplicateBranchesInSwitch")
    protected Object readAtom(AtomTypeInfo info, BinaryReader reader, TypeFactory factory) throws IOException {
        return switch (info.name().name()) {
            // Base types
            case "bool" -> reader.readByteBoolean();
            case "wchar" -> (char) reader.readShort();
            case "uint8", "int8" -> reader.readByte();
            case "uint16", "int16" -> reader.readShort();
            case "uint", "int", "uint32", "int32" -> reader.readInt();
            case "uint64", "int64" -> reader.readLong();
            case "uint128", "int128" -> new BigInteger(reader.readBytes(16));
            case "HalfFloat" -> Float.float16ToFloat(reader.readShort());
            case "float" -> reader.readFloat();
            case "double" -> reader.readDouble();
            case "String", "WString" -> {
                var table = atomTables.get(info.name().name());
                var index = readSizeInt(reader, table.size());
                yield table.get(index);
            }

            // Aliases
            case "RenderDataPriority", "MaterialType" -> reader.readShort();
            case "PhysicsCollisionFilterInfo" -> reader.readInt();
            // case "AnimationSet", "AnimationTagID", "PhysicsCollisionFilterInfo" -> reader.readInt();
            // case "Filename" -> readString(reader);

            default -> throw new IllegalArgumentException("Unknown atom type: " + info.name());
        };
    }

    @NotNull
    @Override
    protected Object readContainer(@NotNull ContainerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        var itemInfo = info.itemType().get();
        var itemType = itemInfo.type();
        var count = readVarInt(reader);

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

    @Nullable
    @Override
    protected Ref<?> readPointer(PointerTypeInfo info, BinaryReader reader, TypeFactory factory) throws IOException {
        var kind = reader.readByte();
        var pointer = switch (kind) {
            case 0 -> {
                int index = readSizeInt(reader, objectTypes.size());
                if (index == 0) {
                    yield null;
                } else {
                    yield new LocalRef<>(index - 1);
                }
            }
            case 2 -> {
                var cacheHash = readVarString(reader);
                yield new DependentRef<>(cacheHash);
            }
            default -> throw new NotImplementedException();
        };
        pointers.add(pointer);
        return pointer;
    }

    private static Object deserializeAtom(String type, BinaryReader reader) throws IOException {
        return switch (type) {
            case "String" -> readVarString(reader);
            default -> throw new IllegalArgumentException("Unknown atom type: " + type);
        };
    }

    private static int readSizeInt(BinaryReader reader, int count) throws IOException {
        if (count <= 255) {
            return Byte.toUnsignedInt(reader.readByte());
        } else if (count <= 65535) {
            return Short.toUnsignedInt(reader.readShort());
        } else {
            return reader.readInt();
        }
    }

    private static int readVarInt(BinaryReader reader) throws IOException {
        int value = Byte.toUnsignedInt(reader.readByte());
        return switch (value) {
            case 0x80 -> reader.readInt();
            case 0x81 -> Short.toUnsignedInt(reader.readShort());
            default -> value;
        };
    }

    private static String readVarString(BinaryReader reader) throws IOException {
        int length = readVarInt(reader);
        if (length == 0) {
            return "";
        }
        return reader.readString(length, StandardCharsets.ISO_8859_1);
    }

    private record Header(
        int pointerMapSize,
        int allocationCount,
        int vramAllocationCount,
        int requiredBinCount,
        int requiredVramBinCount
    ) {
        static Header read(BinaryReader reader) throws IOException {
            var version = reader.readString(15);
            if (!version.equals("RTTIBin<1.73>  ")) {
                throw new IllegalStateException("Unsupported version: " + version);
            }
            var endian = reader.readByte();
            switch (endian) {
                case 0 -> reader.order(ByteOrder.LITTLE_ENDIAN);
                case 1 -> reader.order(ByteOrder.BIG_ENDIAN);
                default -> throw new IllegalStateException("Unsupported endian: " + endian);
            }
            var pointerMapSize = reader.readInt();
            var allocationCount = reader.readInt();
            var vramAllocationCount = reader.readInt();
            var requiredBinCount = Short.toUnsignedInt(reader.readShort());
            var requiredVramBinCount = Short.toUnsignedInt(reader.readShort());
            return new Header(pointerMapSize, allocationCount, vramAllocationCount, requiredBinCount, requiredVramBinCount);
        }
    }

    private record ObjectHeader(byte[] hash, int size) {
        static ObjectHeader read(BinaryReader reader) throws IOException {
            var hash = reader.readBytes(16);
            var size = reader.readInt();
            return new ObjectHeader(hash, size);
        }
    }

    private record Allocation(int size, int alignment, int unk2, int unk3, int unk4) {
        static Allocation read(BinaryReader reader) throws IOException {
            var size = readVarInt(reader);
            var alignment = readVarInt(reader);
            var unk2 = readVarInt(reader);
            var unk3 = readVarInt(reader);
            var unk4 = readVarInt(reader);
            return new Allocation(size, alignment, unk2, unk3, unk4);
        }
    }

    private record AtomTable(String type, List<Object> entries) {
        static AtomTable read(BinaryReader reader, List<String> typeInfo) throws IOException {
            var type = typeInfo.get(readVarInt(reader));
            var count = readVarInt(reader);
            var entries = reader.readObjects(count, r -> {
                var data = r.readBytes(readVarInt(r));
                return deserializeAtom(type, BinaryReader.wrap(data));
            });

            return new AtomTable(type, entries);
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

    private record DependentRef<T>(
        String cacheHash
    ) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }
}
