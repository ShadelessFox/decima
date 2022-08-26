package com.shade.decima.ui.data.viewer.texture;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static com.shade.decima.ui.data.viewer.texture.TextureReaderBC3.getColorsBC3;

public class TextureReaderBC4 extends TextureReader {
    public static class Provider implements TextureReaderProvider {
        @NotNull
        @Override
        public TextureReader create(int width, int height, @NotNull String format) {
            return new TextureReaderBC4(width, height, format.equals("BC4S"));
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC4U") || format.equals("BC4S");
        }
    }

    private final boolean signed;

    public TextureReaderBC4(int width, int height, boolean signed) {
        super(width, height, BufferedImage.TYPE_BYTE_GRAY, 4, 4);
        this.signed = signed;
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final var reds = getColorsBC3(buffer);

        for (int i = 0; i < 16; i++) {
            final int color = reds.apply(i);

            image.setRGB(x + i % 4, y + i / 4, color << 16 | color << 8 | color);
        }
    }
}
