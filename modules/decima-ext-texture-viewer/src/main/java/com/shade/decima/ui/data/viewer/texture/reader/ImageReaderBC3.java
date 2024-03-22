package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.RGB;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderBC3 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC3();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC3");
        }
    }

    public ImageReaderBC3() {
        super(8, 4, CM_INT_ARGB);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final var alphas = getColorsBC3(buffer);
        final var colors = ImageReaderBC1.getColorsBC1(buffer);

        for (int i = 0; i < 16; i++) {
            final var alpha = alphas.apply(i);
            final var color = colors.apply(i).a(alpha);

            image.setRGB(x + (i % 4), y + (i / 4), color.argb());
        }
    }

    @NotNull
    public static RGB.PixelMapper getColorsBC3(@NotNull ByteBuffer buffer) {
        final var colors = new int[8];
        final var color0 = colors[0] = buffer.get() & 0xff;
        final var color1 = colors[1] = buffer.get() & 0xff;
        final var indices = Integer.toUnsignedLong(buffer.getInt()) | Short.toUnsignedLong(buffer.getShort()) << 32;

        if (color0 > color1) {
            colors[2] = RGB.mix(color0, color1, 6f / 7f);
            colors[3] = RGB.mix(color0, color1, 5f / 7f);
            colors[4] = RGB.mix(color0, color1, 4f / 7f);
            colors[5] = RGB.mix(color0, color1, 3f / 7f);
            colors[6] = RGB.mix(color0, color1, 2f / 7f);
            colors[7] = RGB.mix(color0, color1, 1f / 7f);
        } else {
            colors[2] = RGB.mix(color0, color1, 4f / 5f);
            colors[3] = RGB.mix(color0, color1, 3f / 5f);
            colors[4] = RGB.mix(color0, color1, 2f / 5f);
            colors[5] = RGB.mix(color0, color1, 1f / 5f);
            colors[6] = 0;
            colors[7] = 255;
        }

        return pixel -> colors[(int) (indices >>> pixel * 3 & 7)];
    }
}
