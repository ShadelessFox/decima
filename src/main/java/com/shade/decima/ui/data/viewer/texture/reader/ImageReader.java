package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
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

    protected abstract void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y);

    protected abstract BufferedImage createImage(int width, int height);

    public int getPixelBits() {
        return pixelBits;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
