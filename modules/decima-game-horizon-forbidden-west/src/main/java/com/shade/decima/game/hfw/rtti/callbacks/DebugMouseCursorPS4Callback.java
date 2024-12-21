package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class DebugMouseCursorPS4Callback implements ExtraBinaryDataCallback<DebugMouseCursorPS4Callback.DebugMouseCursorData> {
    public interface DebugMouseCursorData {
        @Attr(name = "Height", type = "uint32", position = 0, offset = 0)
        int height();

        void height(int value);

        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull DebugMouseCursorData object) throws IOException {
        object.height(reader.readInt());
        object.data(reader.readBytes(reader.readInt()));
    }
}
