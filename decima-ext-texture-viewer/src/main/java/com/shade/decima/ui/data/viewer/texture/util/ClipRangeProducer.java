package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

import java.awt.image.*;
import java.util.Hashtable;

public class ClipRangeProducer implements ImageProducer {
    private final BufferedImage image;
    private final Hashtable<?, ?> properties;
    private final float lowerRange;
    private final float upperRange;

    private ImageConsumer consumer;

    public ClipRangeProducer(@NotNull BufferedImage image, float lowerRange, float upperRange) {
        this.image = image;
        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
        this.properties = new Hashtable<>();
    }

    @Override
    public void addConsumer(ImageConsumer ic) {
        consumer = ic;
        produce();
    }

    @Override
    public boolean isConsumer(ImageConsumer ic) {
        return ic == consumer;
    }

    @Override
    public void removeConsumer(ImageConsumer ic) {
        if (ic == consumer) {
            consumer = null;
        }
    }

    @Override
    public void startProduction(ImageConsumer ic) {
        addConsumer(ic);
    }

    @Override
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // do nothing
    }

    private void produce() {
        consumer.setDimensions(image.getWidth(), image.getHeight());
        consumer.setProperties(properties);
        sendPixels();
        consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);

        if (consumer != null) {
            consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
        }
    }

    private void sendPixels() {
        final WritableRaster raster = image.getRaster();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final float[] srcScanline = new float[width * raster.getNumDataElements()];
        final int[] dstScanline = new int[width];

        for (int y = 0; y < height; y++) {
            raster.getDataElements(0, y, width, 1, srcScanline);

            for (int x = 0; x < width; x++) {
                final int r = (int) (clip(srcScanline[x * 3], lowerRange, upperRange) * 255) & 0xff;
                final int g = (int) (clip(srcScanline[x * 3 + 1], lowerRange, upperRange) * 255) & 0xff;
                final int b = (int) (clip(srcScanline[x * 3 + 2], lowerRange, upperRange) * 255) & 0xff;
                dstScanline[x] = r << 16 | g << 8 | b;
            }

            consumer.setPixels(0, y, width, 1, ColorModel.getRGBdefault(), dstScanline, 0, width);
        }
    }

    private static float clip(float value, float low, float high) {
        return (Math.clamp(value, low, high) - low) / (high - low);
    }
}
