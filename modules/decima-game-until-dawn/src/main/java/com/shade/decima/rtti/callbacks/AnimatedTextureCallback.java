package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class AnimatedTextureCallback implements ExtraBinaryDataCallback<AnimatedTextureCallback.AnimatedTextureData> {
    public interface AnimatedTextureData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull AnimatedTextureData object) throws IOException {
    }
}
