package com.shade.decima.rtti.data;

import com.shade.decima.rtti.RTTI;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public interface DataSource {
    @RTTI.Attr(name = "Location", type = "String", position = 0, offset = 0)
    String location();

    void location(String value);

    @RTTI.Attr(name = "Offset", type = "uint64", position = 1, offset = 0)
    long offset();

    void offset(long value);

    @RTTI.Attr(name = "Length", type = "uint64", position = 2, offset = 0)
    long length();

    void length(long value);

    @NotNull
    static DataSource read(@NotNull BinaryReader reader) throws IOException {
        DataSource source = RTTI.newInstance(DataSource.class);
        source.location(reader.readString(reader.readInt()));
        source.offset(reader.readLong());
        source.length(reader.readLong());
        return source;
    }
}
