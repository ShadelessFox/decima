package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC3.getColorsBC3;

public class ImageReaderBC4 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC4(format.equals("BC4S"));
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC4U") || format.equals("BC4S");
        }
    }

    private final boolean signed;

    public ImageReaderBC4(boolean signed) {
        super(4, 4);
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

    @Override
    protected BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    }
}
