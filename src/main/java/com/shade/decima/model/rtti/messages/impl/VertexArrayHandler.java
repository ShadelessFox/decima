package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "VertexArrayResource", message = "MsgReadBinary", game = GameType.DS)
public class VertexArrayHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeEnum EVertexElementStorageType = (RTTITypeEnum) registry.find("EVertexElementStorageType");
        final RTTITypeEnum EVertexElement = (RTTITypeEnum) registry.find("EVertexElement");
        final RTTIType<?> MurmurHashValue = registry.find("MurmurHashValue");

        final RTTITypeClass HwVertexStreamElement = RTTIUtils.newClassBuilder(registry, "HwVertexStreamElement")
            .member("Offset", "uint8")
            .member("StorageType", "EVertexElementStorageType")
            .member("UsedSlots", "uint8")
            .member("Type", "EVertexElement")
            .build();

        final RTTITypeClass HwVertexStream = RTTIUtils.newClassBuilder(registry, "HwVertexStream")
            .member("Flags", "uint32")
            .member("Stride", "uint32")
            .member("ElementsCount", "uint32")
            .member("Elements", new RTTITypeArray<>("Array", HwVertexStreamElement))
            .member("Hash", "MurmurHashValue")
            .member("Data", "Array<uint8>")
            .build();

        final RTTITypeClass HwVertexArray = RTTIUtils.newClassBuilder(registry, "HwVertexArray")
            .member("VertexCount", "uint32")
            .member("StreamCount", "uint32")
            .member("IsStreaming", "bool")
            .member("Streams", new RTTITypeArray<>("Array", HwVertexStream))
            .build();

        final var vertexCount = buffer.getInt();
        final var streamCount = buffer.getInt();
        final var streaming = buffer.get() != 0;
        final var streams = new RTTIObject[streamCount];

        for (int i = 0; i < streamCount; i++) {
            final var flags = buffer.getInt();
            final var stride = buffer.getInt();
            final var elementsCount = buffer.getInt();
            final var elements = new RTTIObject[elementsCount];

            for (int j = 0; j < elementsCount; j++) {
                final var element = elements[j] = HwVertexStreamElement.instantiate();
                element.set("Offset", buffer.get());
                element.set("StorageType", EVertexElementStorageType.valueOf(buffer.get()));
                element.set("UsedSlots", buffer.get());
                element.set("Type", EVertexElement.valueOf(buffer.get()));
            }

            final var hash = MurmurHashValue.read(registry, buffer);
            final var data = streaming ? new byte[0] : IOUtils.getBytesExact(buffer, stride * vertexCount);

            final var stream = streams[i] = HwVertexStream.instantiate();
            stream.set("Flags", flags);
            stream.set("Stride", stride);
            stream.set("ElementsCount", elementsCount);
            stream.set("Elements", elements);
            stream.set("Hash", hash);
            stream.set("Data", data);
        }

        final RTTIObject data = HwVertexArray.instantiate();
        data.set("VertexCount", vertexCount);
        data.set("StreamCount", streamCount);
        data.set("IsStreaming", streaming);
        data.set("Streams", streams);

        object.define("Data", HwVertexArray, data);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
