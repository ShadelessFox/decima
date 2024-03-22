package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public record BufferView(@NotNull Buffer buffer, int offset, int length) {
    public BufferView {
        Objects.checkFromIndexSize(offset, length, buffer.length());
    }

    @NotNull
    public ByteBuffer asByteBuffer() {
        return buffer.asByteBuffer()
            .slice(offset, length)
            .order(ByteOrder.LITTLE_ENDIAN);
    }
}
