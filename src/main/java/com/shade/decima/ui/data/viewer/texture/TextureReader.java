package com.shade.decima.ui.data.viewer.texture;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public abstract class TextureReader {
    protected final int width;
    protected final int height;
    protected final int type;
    protected final int pixelBits;
    protected final int blockSize;

    protected TextureReader(int width, int height, int type, int pixelBits, int blockSize) {
        this.width = IOUtils.roundUp(width, blockSize);
        this.height = IOUtils.roundUp(height, blockSize);
        this.type = type;
        this.pixelBits = pixelBits;
        this.blockSize = blockSize;
    }

    @NotNull
    public BufferedImage read(@NotNull ByteBuffer buffer) {
        final BufferedImage image = new BufferedImage(width, height, type);

        for (int y = 0; y < height; y += blockSize) {
            for (int x = 0; x < width; x += blockSize) {
                readBlock(buffer, image, x, y);
            }
        }

        return image;
    }

    protected abstract void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y);

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixelBits() {
        return pixelBits;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
