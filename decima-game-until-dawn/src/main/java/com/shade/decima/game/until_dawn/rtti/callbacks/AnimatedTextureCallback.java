package com.shade.decima.game.until_dawn.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
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
