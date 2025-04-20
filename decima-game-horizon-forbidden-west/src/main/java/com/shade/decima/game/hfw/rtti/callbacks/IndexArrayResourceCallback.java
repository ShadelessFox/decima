package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.MurmurHashValue;
import com.shade.decima.game.hfw.rtti.data.EIndexFormat;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
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

        @Attr(name = "Format", type = "EIndexFormat", position = 2, offset = 0)
        EIndexFormat format();

        void format(EIndexFormat value);

        @Attr(name = "Checksum", type = "MurmurHashValue", position = 3, offset = 0)
        MurmurHashValue hash();

        void hash(MurmurHashValue value);

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
        var format = EIndexFormat.valueOf(reader.readInt());
        var streaming = reader.readIntBoolean();
        var hash = HFWTypeReader.readCompound(MurmurHashValue.class, reader, factory);
        var data = streaming ? null : reader.readBytes(count * format.stride());

        object.count(count);
        object.flags(flags);
        object.format(format);
        object.streaming(streaming);
        object.hash(hash);
        object.data(data);
    }
}
