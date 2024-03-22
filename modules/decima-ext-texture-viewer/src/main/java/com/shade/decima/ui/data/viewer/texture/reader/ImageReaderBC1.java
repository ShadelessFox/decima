package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.RGB;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public final class ImageReaderBC1 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC1();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC1");
        }
    }

    public ImageReaderBC1() {
        super(4, 4, CM_INT_ARGB);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final var colors = getColorsBC1(buffer);

        for (int pixel = 0; pixel < 16; pixel++) {
            final var color = colors.apply(pixel);

            image.setRGB(x + pixel % 4, y + pixel / 4, color.argb());
        }
    }

    @NotNull
    public static IntFunction<RGB> getColorsBC1(@NotNull ByteBuffer buffer) {
        final var color0 = buffer.getShort() & 0xffff;
        final var color1 = buffer.getShort() & 0xffff;
        final var indices = buffer.getInt();

        final var colors = new RGB[4];
        colors[0] = RGB.from565(color0);
        colors[1] = RGB.from565(color1);

        if (color0 > color1) {
            colors[2] = RGB.mix(colors[0], colors[1], 2f / 3f);
            colors[3] = RGB.mix(colors[0], colors[1], 1f / 3f);
        } else {
            colors[2] = RGB.mix(colors[0], colors[1], 1f / 2f);
            colors[3] = RGB.TRANSPARENT;
        }

        return pixel -> colors[indices >>> pixel * 2 & 3];
    }

}
