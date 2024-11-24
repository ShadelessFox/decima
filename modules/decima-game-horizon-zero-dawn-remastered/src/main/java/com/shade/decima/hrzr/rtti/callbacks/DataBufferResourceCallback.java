package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class DataBufferResourceCallback implements ExtraBinaryDataCallback<DataBufferResourceCallback.DataBufferData> {
    public interface DataBufferData {

    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull DataBufferData object) throws IOException {
        var count = reader.readInt();
        if (count > 0) {
            var streaming = reader.readIntBoolean();
            var format = reader.readInt();
            var flags = reader.readInt();
            var stride = reader.readInt();
            var data = streaming ? null : reader.readBytes(stride * count);
        }
    }
}
