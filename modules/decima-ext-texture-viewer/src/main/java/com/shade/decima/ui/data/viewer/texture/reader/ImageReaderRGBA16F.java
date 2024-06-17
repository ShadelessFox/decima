package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

public class ImageReaderRGBA16F extends PixelImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderRGBA16F();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("RGBA_FLOAT_16");
        }
    }

    protected ImageReaderRGBA16F() {
        super(64, CM_FLOAT_RGBA);
    }

    @Override
    protected void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y) {
        float r = BufferUtils.getHalfFloat(buffer);
        float g = BufferUtils.getHalfFloat(buffer);
        float b = BufferUtils.getHalfFloat(buffer);
        float a = BufferUtils.getHalfFloat(buffer);
        raster.setPixel(x, y, new float[]{r, g, b, a});
    }
}
