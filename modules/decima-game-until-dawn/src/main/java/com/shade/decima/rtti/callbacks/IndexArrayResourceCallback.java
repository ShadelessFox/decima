package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

import static com.shade.decima.rtti.UntilDawn.EIndexFormat;

public class IndexArrayResourceCallback implements ExtraBinaryDataCallback<IndexArrayResourceCallback.Indices> {
    public interface Indices {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "Flags", type = "uint32", position = 1, offset = 0)
        int flags();

        void flags(int value);

        @Attr(name = "Format", type = "EIndexFormat", position = 2, offset = 0)
        EIndexFormat format();

        void format(EIndexFormat value);

        @Attr(name = "Checksum", type = "Array<uint8>", position = 3, offset = 0)
        byte[] checksum();

        void checksum(byte[] value);

        @Attr(name = "Unknown", type = "uint32", position = 4, offset = 0)
        int unknown();

        void unknown(int value);

        @Attr(name = "Data", type = "Array<uint8>", position = 5, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull Indices object) throws IOException {
        object.count(reader.readInt());
        if (object.count() > 0) {
            object.flags(reader.readInt());
            object.format(EIndexFormat.valueOf(reader.readInt()));
            object.checksum(reader.readBytes(16));
            object.unknown(reader.readInt());
            object.data(reader.readBytes(switch (object.format()) {
                case Index16 -> object.count() * Short.BYTES;
                case Index32 -> object.count() * Integer.BYTES;
            }));
        }
    }
}
