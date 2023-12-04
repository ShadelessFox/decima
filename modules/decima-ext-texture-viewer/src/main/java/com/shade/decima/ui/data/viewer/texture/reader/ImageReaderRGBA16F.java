package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderRGBA16F extends ImageReader {
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
        super(64, 1, CM_FLOAT_RGBA);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final float r = IOUtils.halfToFloat(buffer.getShort());
        final float g = IOUtils.halfToFloat(buffer.getShort());
        final float b = IOUtils.halfToFloat(buffer.getShort());
        final float a = IOUtils.halfToFloat(buffer.getShort());

        image.getRaster().setDataElements(x, y, new float[]{r, g, b, a});
    }
}
