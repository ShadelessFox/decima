package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "IndexArrayResource", message = "MsgReadBinary", game = GameType.DS)
public class IndexArrayResourceHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass HwIndexArray = RTTIUtils.newClassBuilder(registry, "HwIndexArray")
            .member("IndexCount", "uint32")
            .member("Flags", "uint32")
            .member("Format", "EIndexFormat")
            .member("IsStreaming", "bool")
            .member("Hash", "MurmurHashValue")
            .member("Data", "Array<uint8>")
            .build();

        final var indexCount = buffer.getInt();
        final var flags = buffer.getInt();
        final var format = ((RTTITypeEnum) registry.find("EIndexFormat")).valueOf(buffer.getInt());
        final var streaming = buffer.getInt() != 0;
        final var hash = registry.find("MurmurHashValue").read(registry, buffer);
        final var data = streaming ? new byte[0] : IOUtils.getBytesExact(buffer, indexCount * getSize(format.toString()));

        final RTTIObject array = HwIndexArray.instantiate();
        array.set("IndexCount", indexCount);
        array.set("Flags", flags);
        array.set("Format", format);
        array.set("IsStreaming", streaming);
        array.set("Hash", hash);
        array.set("Data", data);

        object.define("Data", HwIndexArray, array);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }

    private static int getSize(@NotNull String format) {
        return switch (format) {
            case "Index16" -> Short.BYTES;
            case "Index32" -> Integer.BYTES;
            default -> throw new IllegalArgumentException(format);
        };
    }
}
