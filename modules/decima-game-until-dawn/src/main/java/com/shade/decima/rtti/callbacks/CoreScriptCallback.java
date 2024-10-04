package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class CoreScriptCallback implements ExtraBinaryDataCallback<CoreScriptCallback.LuaScript> {
    public interface LuaScript {

    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull LuaScript object) {
        var flags = buffer.getInt();
        if (flags != 0) {
            var name1 = BufferUtils.getPString(buffer);
            var name2 = BufferUtils.getPString(buffer);
            if ((flags & 0xFFFFFFFE) == 2) {
                var header = LuaHeader.read(buffer);
                var chunk = LuaChunk.read(buffer);
                return;
            }
        }
    }

    public sealed interface LuaConstant {
        @NotNull
        static LuaConstant read(@NotNull ByteBuffer buffer) {
            byte type = buffer.get();
            return switch (type) {
                case 0x00 -> new Nil();
                case 0x01 -> new Bool(BufferUtils.getByteBoolean(buffer));
                case 0x03 -> new Num(buffer.getInt());
                case 0x04 -> new Str(BufferUtils.getPString(buffer));
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
        static LuaLocal read(@NotNull ByteBuffer buffer) {
            var name = BufferUtils.getPString(buffer);
            var start = buffer.getInt();
            var end = buffer.getInt();
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
        public static LuaChunk read(@NotNull ByteBuffer buffer) {
            var name = BufferUtils.getPString(buffer);
            var firstLine = buffer.getInt();
            var lastLine = buffer.getInt();
            var upvalCount = buffer.get();
            var paramCount = buffer.get();
            var vararg = buffer.get();
            var maxStack = buffer.get();
            var instructions = BufferUtils.getInts(buffer, buffer.getInt());
            var constants = BufferUtils.getObjects(buffer, buffer.getInt(), LuaConstant[]::new, LuaConstant::read);
            var protos = BufferUtils.getObjects(buffer, buffer.getInt(), LuaChunk[]::new, LuaChunk::read);
            var lines = BufferUtils.getInts(buffer, buffer.getInt());
            var locals = BufferUtils.getObjects(buffer, buffer.getInt(), LuaLocal[]::new, LuaLocal::read);
            var upvalues = BufferUtils.getObjects(buffer, buffer.getInt(), String[]::new, BufferUtils::getPString);

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
        public static LuaHeader read(@NotNull ByteBuffer buffer) {
            return new LuaHeader(
                buffer.getInt(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            );
        }
    }
}
