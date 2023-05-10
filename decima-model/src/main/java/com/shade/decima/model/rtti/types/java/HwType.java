package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface HwType {
    void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer);

    int getSize();
}
