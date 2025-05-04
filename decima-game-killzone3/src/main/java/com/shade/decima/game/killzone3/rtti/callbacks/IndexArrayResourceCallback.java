package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.game.killzone3.rtti.data.EIndexFormat;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotImplementedException;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class IndexArrayResourceCallback implements ExtraBinaryDataCallback<IndexArrayResourceCallback.IndexArrayData> {
    public interface IndexArrayData {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "Flags", type = "uint32", position = 1, offset = 0)
        int flags(); // EResourceCreationFlags

        void flags(int value);

        @Attr(name = "Format", type = "EIndexFormat", position = 2, offset = 0)
        EIndexFormat format();

        void format(EIndexFormat value);

        @Attr(name = "Data", type = "Array<uint8>", position = 5, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, IndexArrayData object) throws IOException {
        reader.align(4);
        object.count(reader.readInt());
        if (object.count() != 0) {
            object.flags(reader.readInt());
            object.format(EIndexFormat.valueOf(reader.readInt()));
            object.data(reader.readBytes(object.count() * object.format().stride()));
            reader.align(4);
        }
    }
}
