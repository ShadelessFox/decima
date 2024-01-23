package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface Buffer {
    @NotNull
    BufferView asView(int offset, int length);

    @NotNull
    ByteBuffer asByteBuffer();

    int length();
}
