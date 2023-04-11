package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.RGB;
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
        super(BufferedImage.TYPE_INT_RGB, 16, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final int value = (int) IOUtils.clamp(IOUtils.halfToFloat(buffer.getShort()), 0.0f, 1.0f) * 255;
        image.setRGB(x, y, new RGB(value, value, value).argb());
    }
}
