package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldMapSuperTileCallback implements ExtraBinaryDataCallback<WorldMapSuperTileCallback.WorldMapSuperTileData> {
    public interface WorldMapSuperTileData {
        @Attr(name = "SmallTextures", type = "Array<TextureData>", position = 0, offset = 0)
        List<TextureCallback.TextureData> smallTextures();

        void smallTextures(List<TextureCallback.TextureData> value);

        @Attr(name = "LargeTextures", type = "Array<TextureData>", position = 1, offset = 0)
        List<TextureCallback.TextureData> largeTextures();

        void largeTextures(List<TextureCallback.TextureData> value);

        @Attr(name = "Mask", type = "uint32", position = 2, offset = 0)
        int mask();

        void mask(int value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull WorldMapSuperTileData object) throws IOException {
        var smallTextureSize = reader.readInt();
        var largeTextureSize = reader.readInt();
        var mask = reader.readInt();

        var smallTextures = new ArrayList<TextureCallback.TextureData>(4);
        var largeTextures = new ArrayList<TextureCallback.TextureData>(4);

        if (smallTextureSize > 0) {
            for (int i = 0; i < 4; i++) {
                if ((mask & (1 << i)) != 0) {
                    smallTextures.add(TextureCallback.TextureData.read(reader, factory));
                }
            }
        }

        if (largeTextureSize > 0) {
            for (int i = 0; i < 4; i++) {
                if ((mask & (1 << i)) != 0) {
                    largeTextures.add(TextureCallback.TextureData.read(reader, factory));
                }
            }
        }

        object.smallTextures(smallTextures);
        object.largeTextures(largeTextures);
        object.mask(mask);
    }
}
