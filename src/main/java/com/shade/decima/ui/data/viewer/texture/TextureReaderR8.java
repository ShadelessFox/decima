package com.shade.decima.ui.data.viewer.texture;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class TextureReaderR8 extends TextureReader {
    public static class Provider implements TextureReaderProvider {
        @NotNull
        @Override
        public TextureReader create(@NotNull String format) {
            return new TextureReaderR8();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_UNORM_8");
        }
    }

    protected TextureReaderR8() {
        super(BufferedImage.TYPE_INT_RGB, 8, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final int r = buffer.get() & 0xff;
        image.setRGB(x, y, r << 16 | r << 8 | r);
    }
}
