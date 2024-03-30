package com.shade.decima.model.rtti.types.jolt.shape;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class MeshShape extends Shape {
    private byte[] tree;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        tree = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
    }
}
