package com.shade.decima.game.hfw.rtti;

import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.runtime.*;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.hash.Hashing;
import com.shade.util.io.BinaryReader;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.RTTIRefObject;

public class RTTIBinaryReader implements Closeable {
    public record ObjectInfo(@NotNull RTTIRefObject object, @NotNull ClassTypeInfo info) {}

    private final BinaryReader reader;
    private final TypeFactory factory;

    public RTTIBinaryReader(@NotNull BinaryReader reader, @NotNull TypeFactory factory) {
        this.reader = reader;
        this.factory = factory;
    }

    @NotNull
    public ObjectInfo readObject() throws IOException {
        var hash = reader.readLong();
        var size = reader.readInt();
        var type = factory.get(HFWTypeId.of(hash));

        var start = reader.position();
        var object = readCompound(type);
        var end = reader.position();

        if (end - start != size) {
            throw new IllegalStateException("Size mismatch for " + type.name() + ": " + (end - start) + " != " + size);
        }

        if (!(object instanceof RTTIRefObject refObject)) {
            throw new IllegalStateException("Expected RTTIRefObject, got " + type.name());
        }

        return new ObjectInfo(refObject, type);
    }

    @NotNull
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
    @SuppressWarnings("DuplicateBranchesInSwitch")
    private Object readAtom(@NotNull AtomTypeInfo info) throws IOException {
        return switch (info.name().name()) {
            // Simple types
            case "bool" -> reader.readByteBoolean();
            case "wchar", "tchar" -> (char) reader.readShort();
            case "uint8", "int8" -> reader.readByte();
            case "uint16", "int16" -> reader.readShort();
            case "uint", "int", "uint32", "int32", "ucs4" -> reader.readInt();
            case "uint64", "int64" -> reader.readLong();
            case "HalfFloat" -> Float.float16ToFloat(reader.readShort());
            case "float" -> reader.readFloat();
            case "double" -> reader.readDouble();

            // Dynamic types
            case "String" -> readString(reader);
            case "WString" -> readWString(reader);

            // Aliases
            case "AnimationEventID" -> reader.readInt();
            case "AnimationNodeID" -> reader.readShort();
            case "AnimationSet" -> reader.readInt();
            case "AnimationTagID" -> reader.readInt();
            case "ClientID" -> reader.readByte();
            case "EntitySoundID" -> reader.readInt();
            case "EntitySoundParamID" -> reader.readInt();
            case "Filename" -> readString(reader);
            case "JointTransformList" -> reader.readLong();
            case "LinearGainFloat" -> reader.readFloat();
            case "MaterialType" -> reader.readShort();
            case "MusicTime" -> reader.readLong();
            case "PhysicsCollisionFilterInfo" -> reader.readInt();
            case "ProgramParameterHandle" -> reader.readInt();
            case "RelativeGainInt" -> reader.readInt();
            case "RenderEffectFeatureSet" -> reader.readByte();
            case "SoundGroupIndex" -> reader.readByte();
            case "SoundVoicePluginId" -> reader.readInt();
            case "TemplateWaveNodeEnumValue" -> reader.readInt();

            default -> throw new IllegalArgumentException("Unknown atom type: " + info.name());
        };
    }

    @NotNull
    private Object readEnum(@NotNull EnumTypeInfo info) throws IOException {
        throw new NotImplementedException();
    }

    @NotNull
    private Object readCompound(@NotNull ClassTypeInfo info) throws IOException {
        Object object = info.newInstance();
        for (ClassAttrInfo attr : info.serializableAttrs()) {
            attr.set(object, readType(attr.type().get()));
        }
        return object;
    }

    @NotNull
    private Object readContainer(@NotNull ContainerTypeInfo info) throws IOException {
        return switch (info.name().name()) {
            case "HashMap", "HashSet" -> readHashContainer(info);
            default -> readSimpleContainer(info);
        };
    }

    @NotNull
    private Object readSimpleContainer(@NotNull ContainerTypeInfo info) throws IOException {
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
    private Object readHashContainer(@NotNull ContainerTypeInfo info) throws IOException {
        throw new NotImplementedException();
    }

    @NotNull
    private Object readPointer(@NotNull PointerTypeInfo info) throws IOException {
        throw new NotImplementedException();
    }

    @NotNull
    private static String readString(@NotNull BinaryReader reader) throws IOException {
        var length = reader.readInt();
        if (length == 0) {
            return "";
        }
        var hash = reader.readInt();
        var data = reader.readBytes(length);
        if (hash != Hashing.decimaCrc32().hashBytes(data).asInt()) {
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

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
