package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.*;

public class WaveResourceCallback implements ExtraBinaryDataCallback<WaveResourceCallback.WaveData> {
    public interface WaveData {
        @Attr(name = "DataSource", type = "StreamingDataSource", position = 1, offset = 0)
        StreamingDataSource dataSource();

        void dataSource(StreamingDataSource value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull WaveData object) throws IOException {
        // TODO: Think about how to pass the host object as T
        var resource = (WaveResource) object;
        if (resource.format().isStreaming()) {
            var dataSource = factory.newInstance(StreamingDataSource.class);
            dataSource.channel(reader.readByte());
            dataSource.offset(reader.readInt());
            dataSource.length(reader.readInt());

            object.dataSource(dataSource);
        }
    }
}
