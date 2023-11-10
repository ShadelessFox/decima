package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

public abstract class ImageReader {
    protected final int pixelBits;
    protected final int blockSize;

    protected ImageReader(int pixelBits, int blockSize) {
        this.pixelBits = pixelBits;
        this.blockSize = blockSize;
    }

    @NotNull
    public BufferedImage read(@NotNull ByteBuffer buffer, int width, int height) {
        final int alignedWidth = IOUtils.alignUp(width, blockSize);
        final int alignedHeight = IOUtils.alignUp(height, blockSize);
        final BufferedImage image = createImage(alignedWidth, alignedHeight);

        for (int y = 0; y < alignedHeight; y += blockSize) {
            for (int x = 0; x < alignedWidth; x += blockSize) {
                readBlock(buffer, image, x, y);
            }
        }

        if (alignedWidth == width && alignedHeight == height) {
            return image;
        } else {
            return image.getSubimage(0, 0, width, height);
        }
    }

    @NotNull
    protected abstract BufferedImage createImage(int width, int height);

    protected abstract void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y);

    public int getPixelBits() {
        return pixelBits;
    }

    public int getBlockSize() {
        return blockSize;
    }

    @NotNull
    protected static BufferedImage createFloatImage(int width, int height) {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final ComponentColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    @NotNull
    protected static BufferedImage createTypedImage(int width, int height, int imageType) {
        return new BufferedImage(width, height, imageType);
    }
}
