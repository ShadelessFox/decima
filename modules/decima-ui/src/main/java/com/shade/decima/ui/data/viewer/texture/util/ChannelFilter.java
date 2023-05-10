package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

import java.awt.image.RGBImageFilter;
import java.util.EnumSet;

public class ChannelFilter extends RGBImageFilter {
    private final EnumSet<Channel> channels;

    public ChannelFilter(@NotNull EnumSet<Channel> channels) {
        this.channels = channels;
        this.canFilterIndexColorModel = true;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int result = 0xff000000;

        if (channels.size() > 1) {
            for (Channel channel : channels) {
                result = channel.setComponent(result, channel.getComponent(rgb));
            }
        } else {
            final Channel channel = channels.iterator().next();
            final int color = channel.getComponent(rgb);
            result |= color << 16 | color << 8 | color;
        }

        return result;
    }
}
