package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExternalSourceCacheResourceCallback implements ExtraBinaryDataCallback<ExternalSourceCacheResourceCallback.ExternalSourceList> {
    public interface ExternalSourceList {
        @Attr(name = "Sources", type = "Array<ExternalSource>", position = 0, offset = 0)
        List<ExternalSource> sources();

        void sources(List<ExternalSource> value);
    }

    public interface ExternalSource {
        @Attr(name = "Name", type = "String", position = 0, offset = 0)
        String name();

        void name(String value);

        @Attr(name = "Data", type = "Array<uint8>", position = 1, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull ExternalSourceList object) throws IOException {
        var count = reader.readInt();
        var sources = new ArrayList<ExternalSource>(count);

        for (int i = 0; i < count; i++) {
            ExternalSource source = factory.newInstance(ExternalSource.class);
            source.name(reader.readString(reader.readInt()));
            source.data(reader.readBytes(reader.readInt()));
            sources.add(source);
        }

        object.sources(sources);
    }
}
