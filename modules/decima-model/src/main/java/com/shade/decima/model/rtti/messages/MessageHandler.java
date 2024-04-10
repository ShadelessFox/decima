package com.shade.decima.model.rtti.messages;

import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public sealed interface MessageHandler permits MessageHandler.ReadBinary {
    non-sealed interface ReadBinary extends MessageHandler {
        record Component(@NotNull String name, @NotNull RTTIType<?> type) {}

        void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer);

        void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer);

        int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory);

        @NotNull
        Component[] components(@NotNull RTTIFactory factory);
    }
}
