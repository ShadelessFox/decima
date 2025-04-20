package com.shade.decima.game.killzone4.rtti.callbacks;

import com.shade.decima.game.killzone4.rtti.data.StreamingDataSource;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

import static com.shade.decima.game.killzone4.rtti.Killzone4.WaveResource;

public class WaveResourceCallback implements ExtraBinaryDataCallback<WaveResourceCallback.WaveData> {
    public interface WaveData {
        @Attr(name = "EmbeddedData", type = "Array<uint8>", position = 1, offset = 0)
        byte[] embeddedData();

        void embeddedData(byte[] value);

        @Attr(name = "StreamingDataSource", type = "StreamingDataSource", position = 1, offset = 0)
        StreamingDataSource streamingDataSource();

        void streamingDataSource(StreamingDataSource value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull WaveData object) throws IOException {
        // TODO: Think about how to pass the host object as T
        var resource = (WaveResource) object;
        var size = reader.readInt();

        if (resource.format().isStreaming()) {
            object.streamingDataSource(StreamingDataSource.read(reader, factory));
        } else {
            object.embeddedData(reader.readBytes(size));
        }
    }
}
