package com.shade.util.lua;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record LuaHeader(
    int signature,
    byte version,
    byte format,
    byte endianness,
    byte sizeOfInt,
    byte sizeOfSize,
    byte sizeOfInstruction,
    byte sizeOfNumber,
    byte integralFlag
) {
    /** mark for precompiled code (`<esc>Lua') */
    public static final int LUA_SIGNATURE = 0x1B | 'L' << 8 | 'u' << 16 | 'a' << 24;
    /** for header of binary files -- this is Lua 5.1 */
    public static final int LUAC_VERSION = 0x51;
    /** for header of binary files -- this is the official format */
    public static final int LUAC_FORMAT = 0;

    public LuaHeader {
        if (signature != LUA_SIGNATURE) {
            throw new IllegalArgumentException("Invalid lua signature");
        }
        if (version != LUAC_VERSION) {
            throw new IllegalArgumentException("Invalid luac version");
        }
        if (format != LUAC_FORMAT) {
            throw new IllegalArgumentException("Invalid luac format");
        }
        if (sizeOfInt != Integer.BYTES) {
            throw new IllegalArgumentException("Invalid size of int");
        }
        if (sizeOfSize != Integer.BYTES) {
            throw new IllegalArgumentException("Invalid size of long");
        }
        if (sizeOfInstruction != Integer.BYTES) {
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
