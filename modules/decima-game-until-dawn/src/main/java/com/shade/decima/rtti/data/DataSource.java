package com.shade.decima.rtti.data;

import com.shade.decima.rtti.RTTI;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

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
    static DataSource read(@NotNull ByteBuffer buffer) {
        DataSource source = RTTI.newInstance(DataSource.class);
        source.location(BufferUtils.getString(buffer, buffer.getInt()));
        source.offset(buffer.getLong());
        source.length(buffer.getLong());
        return source;
    }
}
