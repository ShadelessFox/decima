package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.MathUtils;
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
        final float r = MathUtils.halfToFloat(buffer.getShort());
        final float g = MathUtils.halfToFloat(buffer.getShort());
        final float b = MathUtils.halfToFloat(buffer.getShort());
        final float a = MathUtils.halfToFloat(buffer.getShort());

        image.getRaster().setDataElements(x, y, new float[]{r, g, b, a});
    }
}
