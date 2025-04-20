package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;

public abstract class ImageReader {
    private static final ColorSpace CS_sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    static final ColorModel CM_INT_RGB = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x00000000);
    static final ColorModel CM_INT_ARGB = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
    static final ColorModel CM_FLOAT_RGB = new ComponentColorModel(CS_sRGB, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
    static final ColorModel CM_FLOAT_RGBA = new ComponentColorModel(CS_sRGB, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_FLOAT);

    private final int pixelBits;
    private final int blockSize;
    private final ColorModel colorModel;

    protected ImageReader(int pixelBits, int blockSize, @NotNull ColorModel colorModel) {
        this.pixelBits = pixelBits;
        this.blockSize = blockSize;
        this.colorModel = colorModel;
    }

    @NotNull
    public abstract BufferedImage read(@NotNull ByteBuffer buffer, int width, int height);

    public int getPixelBits() {
        return pixelBits;
    }

    public int getBlockSize() {
        return blockSize;
    }

    @NotNull
    BufferedImage createImage(int width, int height) {
        return new BufferedImage(
            colorModel,
            colorModel.createCompatibleWritableRaster(width, height),
            colorModel.isAlphaPremultiplied(),
            null
        );
    }
}
