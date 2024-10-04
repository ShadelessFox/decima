package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class AnimatedTextureCallback implements ExtraBinaryDataCallback<AnimatedTextureCallback.AnimatedTextureData> {
    public interface AnimatedTextureData {
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull AnimatedTextureData object) {
    }
}
