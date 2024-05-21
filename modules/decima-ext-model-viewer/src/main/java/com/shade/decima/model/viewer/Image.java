package com.shade.decima.model.viewer;

import java.nio.Buffer;
import java.util.Collection;

public interface Image {
    enum ColorSpace {
        LINEAR,
        SRGB
    }

    interface Channel {
        String getName(); // R, G, B, A, etc.

        ColorSpace getColorSpace();

        Buffer getData(); // ByteBuffer, ShortBuffer, etc.
    }

    String getFormatName();

    int getWidth();

    int getHeight();

    Collection<Channel> getChannels();

    // ImageDescriptor(channels=[
    //   (name=R, colorSpace=SRGB, type=uint8),
    //   (name=G, colorSpace=SRGB, type=uint8),
    //   (name=B, colorSpace=SRGB, type=uint8),
    //   (name=A, colorSpace=LINEAR, type=uint8)
    // ])
}
