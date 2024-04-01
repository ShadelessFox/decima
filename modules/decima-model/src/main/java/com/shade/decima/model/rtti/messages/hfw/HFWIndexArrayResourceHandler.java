package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
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
    @Type(name = "IndexArrayResource", game = GameType.HFW),
})
public class HFWIndexArrayResourceHandler implements MessageHandler.ReadBinary {
    private static final int[] unkArray = new int[]{
        0, 0x10, 0x20, 0x40, 0x20, 0x40, 0x60, 0x80,
        0x8, 0x10, 0x20, 0x40, 0x60, 0x80, 0x20, 0x40, 0x60, 0x80,
        0x8, 0x10, 0x20, 0x40, 0x20, 0x20, 0x40, 0x20, 0x20, 0x40, 0x20, 0x20, 0x40, 0x20,
    };

    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var array = new IndexArray();
        array.indexCount = buffer.getInt();
        if (array.indexCount == 0) {
            return;
        }
        array.flags = buffer.getInt();
        array.sizeOrId = buffer.getInt();
        array.streaming = buffer.getInt() != 0;
        final int itemSize;
        if (array.sizeOrId != 0) {
            itemSize = unkArray[array.sizeOrId + 9] / 8;
        } else {
            itemSize = 2;
        }

        array.hash = registry.<RTTIClass>find("MurmurHashValue").read(registry, buffer);
        array.indices = array.streaming ? null : BufferUtils.getBytes(buffer, array.indexCount * itemSize);

        object.set("Data", new RTTIObject(registry.find(IndexArray.class), array));
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
            new Component("Data", registry.find(IndexArray.class))
        };
    }

    public static class IndexArray {
        @RTTIField(type = @Type(name = "uint32"))
        public int indexCount;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "uint32"))
        public int sizeOrId;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public RTTIObject hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] indices;
    }
}
