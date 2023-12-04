package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;

public abstract class ImageReader {
    protected static final ColorSpace CS_sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    protected static final ColorModel CM_INT_RGB = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
    protected static final ColorModel CM_INT_ARGB = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
    protected static final ColorModel CM_FLOAT_RGB = new ComponentColorModel(CS_sRGB, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

    protected final int pixelBits;
    protected final int blockSize;
    protected final ColorModel colorModel;

    protected ImageReader(int pixelBits, int blockSize, @NotNull ColorModel colorModel) {
        this.pixelBits = pixelBits;
        this.blockSize = blockSize;
        this.colorModel = colorModel;
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

    protected abstract void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y);

    public int getPixelBits() {
        return pixelBits;
    }

    public int getBlockSize() {
        return blockSize;
    }

    @NotNull
    public ColorModel getColorModel() {
        return colorModel;
    }

    @NotNull
    private BufferedImage createImage(int width, int height) {
        return new BufferedImage(
            colorModel,
            colorModel.createCompatibleWritableRaster(width, height),
            colorModel.isAlphaPremultiplied(),
            null
        );
    }
}
