package com.shade.decima.game.until_dawn.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import com.shade.util.lua.LuaHeader;
import com.shade.util.lua.LuaProto;

import java.io.IOException;

public class CoreScriptCallback implements ExtraBinaryDataCallback<CoreScriptCallback.Script> {
    public interface Script {
        int STATE_NOT_INITIALISED = 0x00;
        int STATE_INITIALISED = 0x01;
        int STATE_COMPILED = 0x02;
        int STATE_EXECUTED = 0x03;

        @Attr(name = "State", type = "uint32", position = 0, offset = 0)
        int state();

        void state(int value);

        @Attr(name = "ScriptName", type = "String", position = 1, offset = 0)
        String scriptName();

        void scriptName(String value);

        @Attr(name = "Script", type = "String", position = 2, offset = 0)
        String script();

        void script(String value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull Script object) throws IOException {
        var state = reader.readInt();
        if (state == Script.STATE_NOT_INITIALISED) {
            return;
        }

        object.state(state);
        object.scriptName(reader.readString(reader.readInt()));
        object.script(reader.readString(reader.readInt()));

        if (state != Script.STATE_COMPILED && state != Script.STATE_EXECUTED) {
            return;
        }

        // NOTE: Read but not used
        var header = LuaHeader.read(reader);
        var proto = LuaProto.read(reader);
    }
}
