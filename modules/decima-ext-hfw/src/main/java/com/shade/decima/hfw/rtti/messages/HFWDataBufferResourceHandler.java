package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "DataBufferResource", game = GameType.HFW),
})
public class HFWDataBufferResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var data = new DataBuffer();
        data.count = buffer.getInt();
        data.streaming = buffer.getInt() != 0;
        data.flags = buffer.getInt();
        data.format = buffer.getInt();
        data.stride = buffer.getInt();
        data.data = data.streaming ? null : BufferUtils.getBytes(buffer, data.count * data.stride);

        object.set("Data", new RTTIObject(factory.find(DataBuffer.class), data));
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
            new Component("Data", factory.find(DataBuffer.class))
        };
    }

    public static class DataBuffer {
        @RTTIField(type = @Type(name = "uint32"))
        public int count;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "uint32"))
        public int format;
        @RTTIField(type = @Type(name = "uint32"))
        public int stride;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
    }
}
