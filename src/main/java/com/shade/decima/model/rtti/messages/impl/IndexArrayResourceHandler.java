package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
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

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "IndexArrayResource", game = GameType.DS),
    @Type(name = "IndexArrayResource", game = GameType.DSDC)
})
public class IndexArrayResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Data", HwIndexArray.read(registry, buffer));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.obj("Data").<HwIndexArray>cast().write(registry, buffer);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return object.obj("Data").<HwIndexArray>cast().getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Data", registry.find(HwIndexArray.class))
        };
    }

    public static class HwIndexArray {
        @RTTIField(type = @Type(name = "uint32"))
        public int indexCount;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "EIndexFormat"))
        public RTTITypeEnum.Constant format;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public RTTIObject hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwIndexArray();
            object.indexCount = buffer.getInt();
            object.flags = buffer.getInt();
            object.format = registry.<RTTITypeEnum>find("EIndexFormat").valueOf(buffer.getInt());
            object.streaming = buffer.getInt() != 0;
            object.hash = registry.<RTTIClass>find("MurmurHashValue").read(registry, buffer);
            object.data = object.streaming ? new byte[0] : IOUtils.getBytesExact(buffer, object.indexCount * object.getIndexSize());

            return new RTTIObject(registry.find(HwIndexArray.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            buffer.putInt(indexCount);
            buffer.putInt(flags);
            buffer.putInt(format.value());
            buffer.putInt(streaming ? 1 : 0);
            hash.type().write(registry, buffer, hash);
            buffer.put(data);
        }

        public int getSize() {
            return 32 + data.length;
        }

        public int getIndexSize() {
            return switch (format.toString()) {
                case "Index16" -> Short.BYTES;
                case "Index32" -> Integer.BYTES;
                default -> throw new IllegalArgumentException(format.toString());
            };
        }
    }
}
