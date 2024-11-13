package com.shade.decima.rtti.serde;

import com.shade.decima.rtti.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public interface ExtraBinaryDataHolder {
    default void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        throw new UnsupportedOperationException("Missing callback for '"
            + getClass().getInterfaces()[0].getSimpleName()
            + "' required to read extra data at " + reader.position());
    }
}
