package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;


@MessageHandlerRegistration(type = "Texture", message = "MsgReadBinary", game = GameType.DS)
public class TextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Header", HwTextureHeader.read(registry, buffer));
        object.set("Data", HwTextureData.read(registry, buffer));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.obj("Header").<HwTextureHeader>cast().write(registry, buffer);
        object.obj("Data").<HwTextureData>cast().write(registry, buffer);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return HwTextureHeader.getSize() + object.obj("Data").<HwTextureData>cast().getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Header", registry.find(HwTextureHeader.class)),
            new Component("Data", registry.find(HwTextureData.class)),
        };
    }

    public static class HwTextureHeader {
        @RTTIField(type = @Type(name = "ETextureType"))
        public RTTITypeEnum.Constant type;
        @RTTIField(type = @Type(name = "uint16"))
        public short width;
        @RTTIField(type = @Type(name = "uint16"))
        public short height;
        @RTTIField(type = @Type(name = "uint16"))
        public short depth;
        @RTTIField(type = @Type(name = "uint8"))
        public byte totalMipCount;
        @RTTIField(type = @Type(name = "EPixelFormat"))
        public RTTITypeEnum.Constant pixelFormat;
        @RTTIField(type = @Type(name = "uint16"))
        public short unk0;
        @RTTIField(type = @Type(name = "uint32"))
        public int unk1;
        @RTTIField(type = @Type(name = "GGUUID"), name = "UUID")
        public RTTIObject uuid;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwTextureHeader();
            object.type = registry.<RTTITypeEnum>find("ETextureType").valueOf(buffer.getShort());
            object.width = (short) (buffer.getShort() & 0x3fff);
            object.height = (short) (buffer.getShort() & 0x3fff);
            object.depth = buffer.getShort();
            object.totalMipCount = buffer.get();
            object.pixelFormat = registry.<RTTITypeEnum>find("EPixelFormat").valueOf(buffer.get());
            object.unk0 = buffer.getShort();
            object.unk1 = buffer.getInt();
            object.uuid = registry.<RTTIClass>find("GGUUID").read(registry, buffer);

            return new RTTIObject(registry.find(HwTextureHeader.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            buffer.putShort((short) type.value());
            // FIXME: Leftmost bit is zeroed
            buffer.putShort(width);
            buffer.putShort(height);
            buffer.putShort(depth);
            buffer.put(totalMipCount);
            buffer.put((byte) pixelFormat.value());
            buffer.putShort(unk0);
            buffer.putInt(unk1);
            uuid.type().write(registry, buffer, uuid);
        }

        public static int getSize() {
            return 32;
        }
    }

    public static class HwTextureData {
        @RTTIField(type = @Type(name = "uint32"))
        public int remainingDataSize;
        @RTTIField(type = @Type(name = "uint32"))
        public int internalDataSize;
        @RTTIField(type = @Type(name = "uint32"))
        public int externalDataSize;
        @RTTIField(type = @Type(name = "uint32"))
        public int externalMipCount;
        @RTTIField(type = @Type(name = "uint32"))
        public int unk0;
        @RTTIField(type = @Type(name = "uint32"))
        public int unk1;
        @RTTIField(type = @Type(type = HwDataSource.class))
        public RTTIObject externalDataSource;
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] internalData;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new HwTextureData();
            object.remainingDataSize = buffer.getInt();
            object.internalDataSize = buffer.getInt();
            object.externalDataSize = buffer.getInt();
            object.externalMipCount = buffer.getInt();
            object.unk0 = buffer.getInt();
            object.unk1 = buffer.getInt();

            if (object.externalDataSize > 0) {
                object.externalDataSource = HwDataSource.read(registry, buffer);
            }

            if (object.internalDataSize > 0) {
                // HACK: InternalDataSize may be greater than the actual size of remaining data
                object.internalData = IOUtils.getBytesExact(buffer, Math.min(object.internalDataSize, buffer.remaining()));
            }

            return new RTTIObject(registry.find(HwTextureData.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            buffer.putInt(remainingDataSize);
            buffer.putInt(internalDataSize);
            buffer.putInt(externalDataSize);
            buffer.putInt(externalMipCount);
            buffer.putInt(unk0);
            buffer.putInt(unk1);

            if (externalDataSize > 0) {
                externalDataSource.<HwDataSource>cast().write(registry, buffer);
            }

            if (internalDataSize > 0) {
                buffer.put(internalData);
            }
        }

        public int getSize() {
            int size = 24;

            if (externalDataSize > 0) {
                size += externalDataSource.<HwDataSource>cast().getSize();
            }

            if (internalDataSize > 0) {
                size += internalData.length;
            }

            return size;
        }
    }
}
