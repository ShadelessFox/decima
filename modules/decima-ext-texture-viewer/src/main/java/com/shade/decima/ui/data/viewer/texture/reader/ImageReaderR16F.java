package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;

import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;

public class ImageReaderR16F extends PixelImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderR16F();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("R_FLOAT_16");
        }

        @NotNull
        @Override
        public Set<Channel> channels(@NotNull String format) {
            return EnumSet.of(Channel.R);
        }
    }

    public ImageReaderR16F() {
        super(16, CM_FLOAT_RGB);
    }

    @Override
    protected void readPixel(@NotNull ByteBuffer buffer, @NotNull WritableRaster raster, int x, int y) {
        float value = MathUtils.halfToFloat(buffer.getShort());
        raster.setPixel(x, y, new float[]{value, value, value});
    }
}
