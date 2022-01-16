package com.shade.decima.rtti.messages;

import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public interface RTTIMessageReadBinary {
    void read(@NotNull RTTIObject object, @NotNull ByteBuffer buffer);

    void write(@NotNull RTTIObject object, @NotNull ByteBuffer buffer);
}
