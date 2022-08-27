package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderRGBA8 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderRGBA8();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("RGBA_8888");
        }
    }

    protected ImageReaderRGBA8() {
        super(BufferedImage.TYPE_INT_ARGB, 32, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        image.setRGB(x, y, Integer.rotateRight(Integer.reverseBytes(buffer.getInt()), 8));
    }
}
