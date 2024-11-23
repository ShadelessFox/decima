package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class IndexArrayResourceCallback implements ExtraBinaryDataCallback<IndexArrayResourceCallback.IndexArrayData> {
    public interface IndexArrayData {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "Flags", type = "uint32", position = 1, offset = 0)
        int flags();

        void flags(int value);

        @Attr(name = "Stride", type = "uint32", position = 2, offset = 0)
        int stride();

        void stride(int value);

        @Attr(name = "Checksum", type = "Array<uint8>", position = 3, offset = 0)
        byte[] checksum();

        void checksum(byte[] value);

        @Attr(name = "IsStreaming", type = "bool", position = 4, offset = 0)
        boolean streaming();

        void streaming(boolean value);

        @Attr(name = "Data", type = "Array<uint8>", position = 5, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull IndexArrayData object) throws IOException {
        var count = reader.readInt();
        var flags = reader.readInt();
        var stride = reader.readInt() != 0 ? 4 : 2;
        var streaming = reader.readIntBoolean();
        var hash = reader.readBytes(16);
        var data = streaming ? null : reader.readBytes(count * stride);

        object.count(count);
        object.flags(flags);
        object.stride(stride);
        object.streaming(streaming);
        object.checksum(hash);
        object.data(data);
    }
}
