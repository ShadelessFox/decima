package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public interface ExtraBinaryDataCallback<T> {
    void deserialize(@NotNull BinaryReader reader, @NotNull T object) throws IOException;
}
