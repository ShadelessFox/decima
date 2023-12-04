package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderR16F extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR16F();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_FLOAT_16");
        }
    }

    public ImageReaderR16F() {
        super(16, 1, CM_FLOAT_RGB);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final float value = IOUtils.halfToFloat(buffer.getShort());

        image.getRaster().setDataElements(x, y, new float[]{
            value,
            value,
            value
        });
    }
}
