package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class DefaultExtraBinaryDataCallback implements ExtraBinaryDataCallback<DefaultExtraBinaryDataCallback.MissingExtraData> {
    public interface MissingExtraData {
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull MissingExtraData object) {
        throw new UnsupportedOperationException("Missing callback for '"
            + object.getClass().getInterfaces()[0].getSimpleName()
            + "' required to read extra data at " + buffer.position());
    }
}
