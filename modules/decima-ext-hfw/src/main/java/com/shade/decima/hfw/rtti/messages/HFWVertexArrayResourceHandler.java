package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "VertexArrayResource", game = GameType.HFW),
})
public class HFWVertexArrayResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var array = new VertexArray();
        array.vertexCount = buffer.getInt();
        array.streams = new RTTIObject[buffer.getInt()];
        array.streaming = buffer.get() != 0;

        for (int i = 0; i < array.streams.length; i++) {
            final var stream = new VertexStream();
            stream.flags = buffer.getInt();
            stream.stride = buffer.getInt();
            stream.elements = new RTTIObject[buffer.getInt()];

            for (int j = 0; j < stream.elements.length; j++) {
                final var element = new VertexElement();
                element.offset = buffer.get();
                element.format = factory.<RTTITypeEnum>find("ESRTElementFormat").valueOf(buffer.get());
                element.slotsUsed = buffer.get();
                element.type = factory.<RTTITypeEnum>find("EVertexElement").valueOf(buffer.get());
                stream.elements[j] = new RTTIObject(factory.find(VertexElement.class), element);
            }

            stream.hash = factory.<RTTIClass>find("MurmurHashValue").read(factory, reader, buffer);
            stream.data = array.streaming ? null : BufferUtils.getBytes(buffer, array.vertexCount * stream.stride);
            array.streams[i] = new RTTIObject(factory.find(VertexStream.class), stream);
        }

        object.set("Data", new RTTIObject(factory.find(VertexArray.class), array));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Data", factory.find(VertexArray.class))
        };
    }

    public static class VertexArray {
        @RTTIField(type = @Type(name = "uint32"))
        public int vertexCount;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(type = VertexStream[].class))
        public RTTIObject[] streams;
    }

    public static class VertexStream {
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "uint32"))
        public int stride;
        @RTTIField(type = @Type(type = VertexElement[].class))
        public RTTIObject[] elements;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public RTTIObject hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
    }

    public static class VertexElement {
        @RTTIField(type = @Type(name = "uint8"))
        public byte offset;
        @RTTIField(type = @Type(name = "ESRTElementFormat"))
        public RTTITypeEnum.Constant format;
        @RTTIField(type = @Type(name = "uint8"))
        public byte slotsUsed;
        @RTTIField(type = @Type(name = "EVertexElement"))
        public RTTITypeEnum.Constant type;
    }
}