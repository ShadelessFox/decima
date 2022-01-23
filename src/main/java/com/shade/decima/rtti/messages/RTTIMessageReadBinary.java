package com.shade.decima.rtti.messages;

import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public interface RTTIMessageReadBinary {
    void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer);

    void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer);
}
