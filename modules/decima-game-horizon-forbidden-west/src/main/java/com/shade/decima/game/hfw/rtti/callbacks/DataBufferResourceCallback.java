package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class DataBufferResourceCallback implements ExtraBinaryDataCallback<DataBufferResourceCallback.DataBuffer> {
    public interface DataBuffer {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "Streaming", type = "bool", position = 1, offset = 0)
        boolean streaming();

        void streaming(boolean value);

        @Attr(name = "Flags", type = "uint32", position = 2, offset = 0)
        int flags();

        void flags(int value);

        @Attr(name = "Format", type = "uint32", position = 2, offset = 0)
        int format();

        void format(int value);

        @Attr(name = "Stride", type = "uint32", position = 3, offset = 0)
        int stride();

        void stride(int value);

        @Attr(name = "Data", type = "Array<uint8>", position = 4, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull DataBuffer object) throws IOException {
        var count = reader.readInt();
        if (count == 0) {
            return;
        }

        var streaming = reader.readIntBoolean();
        var flags = reader.readInt();
        var format = reader.readInt();
        var stride = reader.readInt();
        var data = streaming ? null : reader.readBytes(stride * count);

        object.count(count);
        object.streaming(streaming);
        object.flags(flags);
        object.format(format);
        object.stride(stride);
        object.data(data);
    }
}
