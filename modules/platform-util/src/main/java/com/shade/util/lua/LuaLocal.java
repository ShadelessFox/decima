package com.shade.util.lua;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record LuaLocal(@NotNull String name, int start, int end) {
    @NotNull
    public static LuaLocal read(@NotNull BinaryReader reader) throws IOException {
        var name = reader.readString(reader.readInt());
        var start = reader.readInt();
        var end = reader.readInt();
        return new LuaLocal(name, start, end);
    }
}
