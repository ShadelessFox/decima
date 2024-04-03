package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
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
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var data = new DataBuffer();
        data.count = buffer.getInt();
        data.streaming = buffer.getInt() != 0;
        data.flags = buffer.getInt();
        data.format = buffer.getInt();
        data.stride = buffer.getInt();
        data.data = data.streaming ? null : BufferUtils.getBytes(buffer, data.count * data.stride);

        object.set("Data", new RTTIObject(registry.find(DataBuffer.class), data));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Data", registry.find(DataBuffer.class))
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
