package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.util.NotNull;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

public class ImageReaderR16 extends PixelImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR16();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_UNORM_16");
        }

        @NotNull
        @Override
        public Set<Channel> channels(@NotNull String format) {
            return EnumSet.of(Channel.R);
        }
    }

    public ImageReaderR16() {
        super(16, CM_FLOAT_RGB);
    }

    @Override
    protected void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y) {
        float value = Short.toUnsignedInt(buffer.getShort()) * (1.0f / 65535.0f);
        raster.setPixel(x, y, new float[]{value, value, value});
    }
}
