package com.shade.decima.hrzr.rtti;

import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class CoreFileReader implements Closeable {
    private final BinaryReader reader;

    public CoreFileReader(@NotNull BinaryReader reader) {
        this.reader = reader;
    }

    @NotNull
    public List<Object> read() throws IOException {
        var hash = reader.readLong();
        var size = reader.readInt();

        throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
