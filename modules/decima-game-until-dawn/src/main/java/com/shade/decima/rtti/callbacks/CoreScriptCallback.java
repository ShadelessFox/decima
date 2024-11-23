package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class CoreScriptCallback implements ExtraBinaryDataCallback<CoreScriptCallback.LuaScript> {
    public interface LuaScript {

    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull LuaScript object) throws IOException {
        var flags = reader.readInt();
        if (flags != 0) {
            var name1 = reader.readString(reader.readInt());
            var name2 = reader.readString(reader.readInt());
            if ((flags & 0xFFFFFFFE) == 2) {
                var header = LuaHeader.read(reader);
                var chunk = LuaChunk.read(reader);
                return;
            }
        }
    }

    public sealed interface LuaConstant {
        @NotNull
        static LuaConstant read(@NotNull BinaryReader reader) throws IOException {
            byte type = reader.readByte();
            return switch (type) {
                case 0x00 -> new Nil();
                case 0x01 -> new Bool(reader.readByteBoolean());
                case 0x03 -> new Num(reader.readInt());
                case 0x04 -> new Str(reader.readString(reader.readInt()));
                default -> throw new IllegalArgumentException("Invalid Lua constant type: " + type);
            };
        }

        record Nil() implements LuaConstant {}

        record Bool(boolean value) implements LuaConstant {}

        record Num(int value) implements LuaConstant {}

        record Str(@NotNull String value) implements LuaConstant {}
    }

    public record LuaLocal(@NotNull String name, int start, int end) {
        @NotNull
        static LuaLocal read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var start = reader.readInt();
            var end = reader.readInt();
            return new LuaLocal(name, start, end);
        }
    }

    public record LuaChunk(
        @NotNull String name,
        int firstLine,
        int lastLine,
        byte upvalCount,
        byte paramCount,
        byte maxStack,
        byte vararg,
        @NotNull int[] instructions,
        @NotNull LuaConstant[] constants,
        @NotNull LuaChunk[] protos,
        @NotNull int[] lines,
        @NotNull LuaLocal[] locals,
        @NotNull String[] upvalues
    ) {
        @NotNull
        public static LuaChunk read(@NotNull BinaryReader reader) throws IOException {
            var name = reader.readString(reader.readInt());
            var firstLine = reader.readInt();
            var lastLine = reader.readInt();
            var upvalCount = reader.readByte();
            var paramCount = reader.readByte();
            var vararg = reader.readByte();
            var maxStack = reader.readByte();
            var instructions = reader.readInts(reader.readInt());
            var constants = reader.readObjects(reader.readInt(), LuaConstant::read, LuaConstant[]::new);
            var protos = reader.readObjects(reader.readInt(), LuaChunk::read, LuaChunk[]::new);
            var lines = reader.readInts(reader.readInt());
            var locals = reader.readObjects(reader.readInt(), LuaLocal::read, LuaLocal[]::new);
            var upvalues = reader.readObjects(reader.readInt(), reader1 -> reader1.readString(reader1.readInt()), String[]::new);

            return new LuaChunk(
                name,
                firstLine,
                lastLine,
                upvalCount,
                paramCount,
                maxStack,
                vararg,
                instructions,
                constants,
                protos,
                lines,
                locals,
                upvalues
            );
        }
    }

    public record LuaHeader(
        int magic,
        byte version,
        byte format,
        byte endianness,
        byte sizeOfInt,
        byte sizeOfSize,
        byte sizeOfInst,
        byte sizeOfNumber,
        byte integralFlag
    ) {
        public static final int LUA_MAGIC = 0x1B | 'L' << 8 | 'u' << 16 | 'a' << 24;
        public static final int LUA_VERSION = 0x51;

        public LuaHeader {
            if (magic != LUA_MAGIC) {
                throw new IllegalArgumentException("Invalid Lua header");
            }
            if (version != LUA_VERSION) {
                throw new IllegalArgumentException("Invalid Lua version");
            }
            if (sizeOfInt != Integer.BYTES) {
                throw new IllegalArgumentException("Invalid size of int");
            }
            if (sizeOfSize != Integer.BYTES) {
                throw new IllegalArgumentException("Invalid size of long");
            }
            if (sizeOfInst != Integer.BYTES) {
                throw new IllegalArgumentException("Invalid size of instruction");
            }
            if (sizeOfNumber != Integer.BYTES) {
                throw new IllegalArgumentException("Invalid size of number");
            }
        }

        @NotNull
        public static LuaHeader read(@NotNull BinaryReader reader) throws IOException {
            return new LuaHeader(
                reader.readInt(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte(),
                reader.readByte()
            );
        }
    }
}
