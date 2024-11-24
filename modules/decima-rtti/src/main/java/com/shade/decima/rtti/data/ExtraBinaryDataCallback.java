package com.shade.decima.rtti.data;

import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public interface ExtraBinaryDataCallback<T> {
    void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull T object) throws IOException;
}
