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

public class ImageReaderR16F extends ImageReader {
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
    }

    public ImageReaderR16F() {
        super(16, 1);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final float value = IOUtils.halfToFloat(buffer.getShort());
        image.getRaster().setDataElements(x, y, new float[]{value, value, value});
    }

    @Override
    protected BufferedImage createImage(int width, int height) {
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final ComponentColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }
}
