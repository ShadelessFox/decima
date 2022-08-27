package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC1.getColorsBC1;

public class ImageReaderBC2 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC2();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC2");
        }
    }

    public ImageReaderBC2() {
        super(BufferedImage.TYPE_INT_ARGB, 8, 4);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final var alphas = buffer.getLong();
        final var colors = getColorsBC1(buffer);

        for (int pixel = 0; pixel < 16; pixel++) {
            final var alpha = (int) (alphas >>> pixel * 4 & 15) * 255 / 15;
            final var color = colors.apply(pixel);

            image.setRGB(x + pixel % 4, y + pixel / 4, alpha << 24 | color.argb());
        }
    }
}
