package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "IndexArrayResource", message = "MsgReadBinary", game = GameType.DS)
public class IndexArrayResourceHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeEnum EIndexFormat = (RTTITypeEnum) registry.find("EIndexFormat");
        final RTTIType<?> GGUUID = registry.find("GGUUID");

        final RTTITypeClass HwIndexArray = RTTIUtils.newClassBuilder(registry, "HwIndexArray")
            .member("IndexCount", "uint32")
            .member("Flags", "uint32")
            .member("Format", "EIndexFormat")
            .member("IsStreaming", "bool")
            .member("UUID", "GGUUID")
            .build();

        final var indexCount = buffer.getInt();
        final var flags = buffer.getInt();
        final var format = EIndexFormat.valueOf(buffer.getInt());
        final var streaming = buffer.getInt() != 0;
        final var uuid = GGUUID.read(registry, buffer);

        final RTTIObject data = HwIndexArray.instantiate();
        data.set("IndexCount", indexCount);
        data.set("Flags", flags);
        data.set("Format", format);
        data.set("IsStreaming", streaming);
        data.set("UUID", uuid);

        object.define("Data", HwIndexArray, data);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
