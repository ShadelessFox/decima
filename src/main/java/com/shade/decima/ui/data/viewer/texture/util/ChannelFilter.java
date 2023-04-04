package com.shade.decima.ui.data.viewer.texture.util;

import java.awt.image.RGBImageFilter;

public class ChannelFilter extends RGBImageFilter {
    private String channel;

    public ChannelFilter(String channel) {
        this.channel = channel;
        canFilterIndexColorModel = true;
    }

    public int filterRGB(int x, int y, int rgb) {
        final int a = (rgb >> 24) & 0xff;
        final int r = (rgb >> 16) & 0xff;
        final int g = (rgb >> 8) & 0xff;
        final int b = (rgb >> 0) & 0xff;
        return switch (this.channel) {
            case "RGB" -> 0xff000000 | r << 16 | g << 8 | b;
            case "R"   -> 0xff000000 | r << 16 | r << 8 | r;
            case "G"   -> 0xff000000 | g << 16 | g << 8 | g;
            case "B"   -> 0xff000000 | b << 16 | b << 8 | b;
            case "A"   ->    a << 24 | 0x00ffffff;
            default -> throw new IllegalArgumentException("channel not in {'RGB', 'R', 'G', 'B', 'A'}");
        };
    }
}
