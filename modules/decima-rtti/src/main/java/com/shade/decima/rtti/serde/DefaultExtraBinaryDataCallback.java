package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class DefaultExtraBinaryDataCallback implements ExtraBinaryDataCallback<DefaultExtraBinaryDataCallback.MissingExtraData> {
    public interface MissingExtraData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull MissingExtraData object) throws IOException {
        throw new UnsupportedOperationException("Missing callback for '"
            + object.getClass().getInterfaces()[0].getSimpleName()
            + "' required to read extra data at " + reader.position());
    }
}
