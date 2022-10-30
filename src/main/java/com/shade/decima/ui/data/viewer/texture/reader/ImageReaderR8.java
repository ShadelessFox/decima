package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderR8 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR8();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_UNORM_8") || format.equals("R_UINT_8");
        }
    }

    protected ImageReaderR8() {
        super(8, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final int r = buffer.get() & 0xff;
        image.setRGB(x, y, r << 16 | r << 8 | r);
    }

    @Override
    protected BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }
}
