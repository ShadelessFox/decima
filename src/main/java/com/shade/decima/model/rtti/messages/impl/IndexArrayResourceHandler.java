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

@MessageHandlerRegistration(type = "IndexArrayResource", message = "MsgReadBinary", game = GameType.DS)
public class IndexArrayResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        object.set("Data", HwIndexArray.read(registry, buffer));
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Data", registry.find(HwIndexArray.class))
        };
    }

    public static class HwIndexArray {
        @RTTIField(type = @Type(name = "uint32"), name = "IndexCount")
        public int indices;
        @RTTIField(type = @Type(name = "uint32"))
        public int flags;
        @RTTIField(type = @Type(name = "EIndexFormat"))
        public Object format;
        @RTTIField(type = @Type(name = "bool"), name = "IsStreaming")
        public boolean streaming;
        @RTTIField(type = @Type(name = "MurmurHashValue"))
        public Object hash;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwIndexArray();
            object.indices = buffer.getInt();
            object.flags = buffer.getInt();
            object.format = ((RTTITypeEnum) registry.find("EIndexFormat")).valueOf(buffer.getInt());
            object.streaming = buffer.getInt() != 0;
            object.hash = registry.find("MurmurHashValue").read(registry, buffer);
            object.data = object.streaming ? new byte[0] : IOUtils.getBytesExact(buffer, object.indices * object.getSize());

            return new RTTIObject(registry.find(HwIndexArray.class), object);
        }

        private int getSize() {
            return switch (format.toString()) {
                case "Index16" -> Short.BYTES;
                case "Index32" -> Integer.BYTES;
                default -> throw new IllegalArgumentException(format.toString());
            };
        }
    }
}
