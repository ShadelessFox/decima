package com.shade.decima.game.killzone4.rtti.data;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public interface StreamingDataSource {
    @Attr(name = "Location", type = "String", position = 0, offset = 0)
    String location();

    void location(String value);

    @Attr(name = "Offset", type = "uint64", position = 1, offset = 0)
    long offset();

    void offset(long value);

    @Attr(name = "Length", type = "uint64", position = 2, offset = 0)
    long length();

    void length(long value);

    @NotNull
    static StreamingDataSource read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        StreamingDataSource source = factory.newInstance(StreamingDataSource.class);
        source.location(reader.readString(reader.readInt()));
        source.offset(reader.readLong());
        source.length(reader.readLong());
        return source;
    }
}
