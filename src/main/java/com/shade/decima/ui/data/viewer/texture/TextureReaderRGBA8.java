package com.shade.decima.ui.data.viewer.texture;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class TextureReaderRGBA8 extends TextureReader {
    public static class Provider implements TextureReaderProvider {
        @NotNull
        @Override
        public TextureReader create(int width, int height, @NotNull String format) {
            return new TextureReaderRGBA8(width, height);
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("RGBA_8888");
        }
    }

    protected TextureReaderRGBA8(int width, int height) {
        super(width, height, BufferedImage.TYPE_INT_ARGB, 32, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        image.setRGB(x, y, Integer.rotateRight(Integer.reverseBytes(buffer.getInt()), 8));
    }
}
