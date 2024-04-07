package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "VertexArrayResource", game = GameType.DS),
    @Type(name = "VertexArrayResource", game = GameType.DSDC)
})
public class DSVertexArrayResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Data", HwVertexArray.read(factory, buffer));
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.obj("Data").<HwVertexArray>cast().write(factory, buffer);
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject object) {
        return object.obj("Data").<HwVertexArray>cast().getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Data", factory.find(HwVertexArray.class))
        };
    }

    public static class HwVertexArray {
        @RTTIField(type = @Type(name = "uint32"))
        public int vertexCount;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(type = HwVertexStream[].class))
        public RTTIObject[] streams;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            final var vertexCount = buffer.getInt();
            final var streamCount = buffer.getInt();
            final var streaming = buffer.get() != 0;
            final var streams = new RTTIObject[streamCount];

            for (int i = 0; i < streamCount; i++) {
                streams[i] = HwVertexStream.read(factory, buffer, streaming, vertexCount);
            }

            final var object = new HwVertexArray();
            object.vertexCount = vertexCount;
            object.streaming = streaming;
            object.streams = streams;

            return new RTTIObject(factory.find(HwVertexArray.class), object);
        }

        public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            buffer.putInt(vertexCount);
            buffer.putInt(streams.length);
            buffer.put((byte) (streaming ? 1 : 0));

            for (RTTIObject stream : streams) {
                stream.<HwVertexStream>cast().write(factory, buffer);
            }
        }

        public int getSize() {
            return 9 + Arrays.stream(streams)
                .map(RTTIObject::<HwVertexStream>cast)
                .mapToInt(HwVertexStream::getSize)
                .sum();
        }
    }

    public static class HwVertexStream {
        @RTTIField(type = @Type(type = HwVertexStreamElement[].class))
        public RTTIObject[] elements;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public RTTIObject hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "uint32"))
        public int stride;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, boolean streaming, int vertices) {
            final var flags = buffer.getInt();
            final var stride = buffer.getInt();
            final var elementsCount = buffer.getInt();
            final var elements = new RTTIObject[elementsCount];

            for (int j = 0; j < elementsCount; j++) {
                elements[j] = HwVertexStreamElement.read(factory, buffer);
            }

            final var object = new HwVertexStream();
            object.flags = flags;
            object.stride = stride;
            object.elements = elements;
            object.hash = factory.<RTTIClass>find("MurmurHashValue").read(factory, buffer);
            object.data = streaming ? new byte[0] : BufferUtils.getBytes(buffer, stride * vertices);

            return new RTTIObject(factory.find(HwVertexStream.class), object);
        }

        public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            buffer.putInt(flags);
            buffer.putInt(stride);
            buffer.putInt(elements.length);

            for (RTTIObject element : elements) {
                element.<HwVertexStreamElement>cast().write(buffer);
            }

            hash.type().write(factory, buffer, hash);
            buffer.put(data);
        }

        public int getSize() {
            return 28 + data.length + elements.length * HwVertexStreamElement.getSize();
        }
    }

    public static class HwVertexStreamElement {
        @RTTIField(type = @Type(name = "EVertexElementStorageType"))
        public RTTITypeEnum.Constant storageType;
        @RTTIField(type = @Type(name = "EVertexElement"))
        public RTTITypeEnum.Constant type;
        @RTTIField(type = @Type(name = "uint8"))
        public byte usedSlots;
        @RTTIField(type = @Type(name = "uint8"))
        public byte offset;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            final var object = new HwVertexStreamElement();
            object.offset = buffer.get();
            object.storageType = factory.<RTTITypeEnum>find("EVertexElementStorageType").valueOf(buffer.get());
            object.usedSlots = buffer.get();
            object.type = factory.<RTTITypeEnum>find("EVertexElement").valueOf(buffer.get());

            return new RTTIObject(factory.find(HwVertexStreamElement.class), object);
        }

        public void write(@NotNull ByteBuffer buffer) {
            buffer.put(offset);
            buffer.put((byte) storageType.value());
            buffer.put(usedSlots);
            buffer.put((byte) type.value());
        }

        public static int getSize() {
            return 4;
        }
    }
}
