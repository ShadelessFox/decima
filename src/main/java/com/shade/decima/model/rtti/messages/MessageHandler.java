package com.shade.decima.model.rtti.messages;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public sealed interface MessageHandler permits MessageHandler.ReadBinary {
    non-sealed interface ReadBinary extends MessageHandler {
        void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer);

        void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer);
    }
}
