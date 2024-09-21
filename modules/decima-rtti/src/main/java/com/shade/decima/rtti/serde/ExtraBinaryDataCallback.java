package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface ExtraBinaryDataCallback<T> {
    void deserialize(@NotNull ByteBuffer buffer, @NotNull T object);
}
