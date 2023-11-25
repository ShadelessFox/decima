package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public interface ImageProvider {
    enum Type {
        TEXTURE,
        CUBEMAP,
        VOLUME
    }

    @NotNull
    BufferedImage getImage(int mip, int slice);

    @NotNull
    ByteBuffer getData(int mip, int slice);

    int getMaxWidth();

    int getMaxHeight();

    int getMipCount();

    int getSliceCount(int mip);

    int getDepth();

    int getArraySize();

    @Nullable
    String getName();

    @NotNull
    Type getType();

    @NotNull
    String getPixelFormat();

    int getBitsPerChannel();
}
