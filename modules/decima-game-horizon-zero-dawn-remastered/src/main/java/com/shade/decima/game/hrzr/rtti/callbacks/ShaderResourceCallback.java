package com.shade.decima.game.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

import static com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered.StreamingDataSource;

public class ShaderResourceCallback implements ExtraBinaryDataCallback<ShaderResourceCallback.ShaderData> {
    public interface ShaderData {
        @Attr(name = "Hash", type = "Array<uint8>", position = 0, offset = 0)
        byte[] hash();

        void hash(byte[] value);

        @Attr(name = "DataSource", type = "StreamingDataSource", position = 1, offset = 0)
        StreamingDataSource dataSource();

        void dataSource(StreamingDataSource value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull ShaderData object) throws IOException {
        var size = reader.readInt();
        var hash = reader.readBytes(16);

        var dataSource = factory.newInstance(StreamingDataSource.class);
        dataSource.channel(reader.readByte());
        dataSource.offset(reader.readInt());
        dataSource.length(reader.readInt());

        assert dataSource.length() == size;

        object.hash(hash);
        object.dataSource(dataSource);
    }
}
