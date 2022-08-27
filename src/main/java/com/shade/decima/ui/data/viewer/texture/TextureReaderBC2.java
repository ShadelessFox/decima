package com.shade.decima.ui.data.viewer.texture;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static com.shade.decima.ui.data.viewer.texture.TextureReaderBC1.getColorsBC1;

public class TextureReaderBC2 extends TextureReader {
    public static class Provider implements TextureReaderProvider {
        @NotNull
        @Override
        public TextureReader create(@NotNull String format) {
            return new TextureReaderBC2();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC2");
        }
    }

    public TextureReaderBC2() {
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
