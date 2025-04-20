package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.util.NotNull;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

public class ImageReaderR8 extends PixelImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR8();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_UNORM_8") || format.equals("R_UINT_8");
        }

        @NotNull
        @Override
        public Set<Channel> channels(@NotNull String format) {
            return EnumSet.of(Channel.R);
        }
    }

    protected ImageReaderR8() {
        super(8, CM_INT_RGB);
    }

    @Override
    protected void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y) {
        int r = buffer.get() & 0xff;
        raster.setPixel(x, y, new int[]{r, r, r});
    }
}
