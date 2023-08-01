package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.hzd.HZDDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "IndexArrayResource", game = GameType.HZD),
})
public class HZDIndexArrayResourceHandler implements MessageHandler.ReadBinary {
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
        @RTTIField(type = @Type(type = HZDDataSource.class))
        public RTTIObject dataSource;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwIndexArray();
            object.indexCount = buffer.getInt();
            object.flags = buffer.getInt();
            object.format = registry.<RTTITypeEnum>find("EIndexFormat").valueOf(buffer.getInt());
            object.streaming = buffer.getInt() != 0;
            object.hash = registry.<RTTIClass>find("MurmurHashValue").read(registry, buffer);

            if (object.streaming) {
                object.dataSource = HZDDataSource.read(registry, buffer);
            } else {
                object.data = BufferUtils.getBytes(buffer, object.indexCount * object.getIndexSize());
            }

            return new RTTIObject(registry.find(HwIndexArray.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            buffer.putInt(indexCount);
            buffer.putInt(flags);
            buffer.putInt(format.value());
            buffer.putInt(streaming ? 1 : 0);
            hash.type().write(registry, buffer, hash);

            if (dataSource != null) {
                dataSource.<HwDataSource>cast().write(registry, buffer);
            } else {
                buffer.put(data);
            }
        }

        public int getSize() {
            int size = 32;

            if (dataSource != null) {
                size += dataSource.<HwDataSource>cast().getSize();
            } else {
                size += data.length;
            }

            return size;
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
