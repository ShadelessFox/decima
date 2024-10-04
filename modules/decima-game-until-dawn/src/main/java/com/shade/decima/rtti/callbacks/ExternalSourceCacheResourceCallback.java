package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ExternalSourceCacheResourceCallback implements ExtraBinaryDataCallback<ExternalSourceCacheResourceCallback.ExternalSourceList> {
    public interface ExternalSourceList {
        @RTTI.Attr(name = "Sources", type = "Array<ExternalSource>", position = 0, offset = 0)
        List<ExternalSource> sources();

        void sources(List<ExternalSource> value);
    }

    public interface ExternalSource {
        @RTTI.Attr(name = "Name", type = "String", position = 0, offset = 0)
        String name();

        void name(String value);

        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull ExternalSourceList object) {
        var count = buffer.getInt();
        var sources = new ArrayList<ExternalSource>(count);

        for (int i = 0; i < count; i++) {
            ExternalSource source = RTTI.newInstance(ExternalSource.class);
            source.name(BufferUtils.getString(buffer, buffer.getInt()));
            source.data(BufferUtils.getBytes(buffer, buffer.getInt()));
            sources.add(source);
        }

        object.sources(sources);
    }
}
