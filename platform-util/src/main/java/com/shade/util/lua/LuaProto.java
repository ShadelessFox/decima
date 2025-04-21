package com.shade.util.lua;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public record LuaProto(
    @NotNull String source,
    int lineDefined,
    int lastLineDefined,
    int numUpvalues,
    int numParameters,
    byte vararg,
    byte maxStack,
    @NotNull int[] code,
    @NotNull List<LuaValue> constants,
    @NotNull List<LuaProto> prototypes,
    @NotNull int[] lines,
    @NotNull List<LuaLocal> locals,
    @NotNull List<String> upvalues
) {
    public static LuaProto read(@NotNull BinaryReader reader) throws IOException {
        var source = reader.readString(reader.readInt());
        var lineDefined = reader.readInt();
        var lastLineDefined = reader.readInt();
        var numUpvalues = Byte.toUnsignedInt(reader.readByte());
        var numParameters = Byte.toUnsignedInt(reader.readByte());
        var vararg = reader.readByte();
        var maxStack = reader.readByte();
        var code = reader.readInts(reader.readInt());
        var constants = reader.readObjects(reader.readInt(), LuaValue::read);
        var prototypes = reader.readObjects(reader.readInt(), LuaProto::read);
        var lines = reader.readInts(reader.readInt());
        var locals = reader.readObjects(reader.readInt(), LuaLocal::read);
        var upvalues = reader.readObjects(reader.readInt(), r -> r.readString(r.readInt()));

        return new LuaProto(
            source,
            lineDefined,
            lastLineDefined,
            numUpvalues,
            numParameters,
            vararg,
            maxStack,
            code,
            constants,
            prototypes,
            lines,
            locals,
            upvalues
        );
    }
}
