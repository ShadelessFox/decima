package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(type = "VertexArrayResource", message = "MsgReadBinary", game = GameType.DS)
public class VertexArrayResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        object.set("Data", HwVertexArray.read(registry, buffer));
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Data", registry.find(HwVertexArray.class))
        };
    }

    public static class HwVertexArray {
        @RTTIField(type = @Type(name = "uint32"), name = "VertexCount")
        public int vertices;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(type = HwVertexStream[].class))
        public Object streams;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var vertexCount = buffer.getInt();
            final var streamCount = buffer.getInt();
            final var streaming = buffer.get() != 0;
            final var streams = new RTTIObject[streamCount];

            for (int i = 0; i < streamCount; i++) {
                streams[i] = HwVertexStream.read(registry, buffer, streaming, vertexCount);
            }

            final var object = new HwVertexArray();
            object.vertices = vertexCount;
            object.streaming = streaming;
            object.streams = streams;

            return new RTTIObject(registry.find(HwVertexArray.class), object);
        }
    }

    public static class HwVertexStream {
        @RTTIField(type = @Type(type = HwVertexStreamElement[].class))
        public Object elements;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public Object hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "uint32"))
        public int stride;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, boolean streaming, int vertices) {
            final var flags = buffer.getInt();
            final var stride = buffer.getInt();
            final var elementsCount = buffer.getInt();
            final var elements = new RTTIObject[elementsCount];

            for (int j = 0; j < elementsCount; j++) {
                elements[j] = HwVertexStreamElement.read(registry, buffer);
            }

            final var object = new HwVertexStream();
            object.flags = flags;
            object.stride = stride;
            object.elements = elements;
            object.hash = registry.find("MurmurHashValue").read(registry, buffer);
            object.data = streaming ? new byte[0] : IOUtils.getBytesExact(buffer, stride * vertices);

            return new RTTIObject(registry.find(HwVertexStream.class), object);
        }
    }

    public static class HwVertexStreamElement {
        @RTTIField(type = @Type(name = "EVertexElementStorageType"))
        public Object storageType;
        @RTTIField(type = @Type(name = "EVertexElement"))
        public Object type;
        @RTTIField(type = @Type(name = "uint32"))
        public byte usedSlots;
        @RTTIField(type = @Type(name = "uint32"))
        public byte offset;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwVertexStreamElement();
            object.offset = buffer.get();
            object.storageType = ((RTTITypeEnum) registry.find("EVertexElementStorageType")).valueOf(buffer.get());
            object.usedSlots = buffer.get();
            object.type = ((RTTITypeEnum) registry.find("EVertexElement")).valueOf(buffer.get());

            return new RTTIObject(registry.find(HwVertexStreamElement.class), object);
        }
    }
}
