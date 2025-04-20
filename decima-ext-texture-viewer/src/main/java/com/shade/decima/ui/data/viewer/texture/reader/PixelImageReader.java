package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

abstract class PixelImageReader extends ImageReader {
    protected PixelImageReader(int pixelBits, @NotNull ColorModel colorModel) {
        super(pixelBits, 1, colorModel);
    }

    @NotNull
    @Override
    public final BufferedImage read(@NotNull ByteBuffer buffer, int width, int height) {
        final BufferedImage image = createImage(width, height);

        for (int y = 0; y < height; y += getBlockSize()) {
            for (int x = 0; x < width; x += getBlockSize()) {
                readPixel(buffer, image.getRaster(), x, y);
            }
        }

        return image;
    }

    abstract void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y);
}
