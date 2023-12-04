package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderR16 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR16();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_UNORM_16");
        }
    }

    public ImageReaderR16() {
        super(16, 1, CM_FLOAT_RGB);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final float value = (buffer.getShort() & 0xffff) / 65535.0f;

        image.getRaster().setDataElements(x, y, new float[]{
            value,
            value,
            value
        });
    }
}
