package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

public class ImageReaderRGBA8 extends PixelImageReader {
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
        super(32, CM_INT_ARGB);
    }

    @Override
    protected void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y) {
        int value = buffer.getInt();
        int a = value >> 24 & 0xff;
        int b = value >> 16 & 0xff;
        int g = value >> 8 & 0xff;
        int r = value & 0xff;
        raster.setPixel(x, y, new int[]{r, g, b, a});
    }
}
