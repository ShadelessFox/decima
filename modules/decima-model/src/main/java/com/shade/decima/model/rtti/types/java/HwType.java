package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface HwType {
    void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer);

    int getSize();
}
