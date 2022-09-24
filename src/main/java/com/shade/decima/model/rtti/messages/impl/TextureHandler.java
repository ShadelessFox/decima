package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;


@RTTIMessageHandler(type = "Texture", message = "MsgReadBinary", game = GameType.DS)
public class TextureHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTIObject hwTextureHeader = readTextureHeader(registry, buffer);
        final RTTIObject hwTexture = readTextureData(registry, buffer);

        object.define("Header", hwTextureHeader.getType(), hwTextureHeader);
        object.define("Data", hwTexture.getType(), hwTexture);
    }

    @NotNull
    public static RTTIObject readTextureHeader(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var textureType = (RTTITypeEnum) registry.find("ETextureType");
        final var pixelFormat = (RTTITypeEnum) registry.find("EPixelFormat");
        final var gguuid = registry.find("GGUUID");

        final RTTIObject hwTextureHeader = RTTIUtils.newClassBuilder(registry, "HwTextureHeader")
            .member("Type", "ETextureType")
            .member("Width", "uint16")
            .member("Height", "uint16")
            .member("Depth", "uint16")
            .member("TotalMipCount", "uint8")
            .member("PixelFormat", "EPixelFormat")
            .member("Unk0", "uint16")
            .member("Unk1", "uint32")
            .member("UUID", "GGUUID")
            .build().instantiate();

        hwTextureHeader.set("Type", textureType.valueOf(buffer.getShort()));
        hwTextureHeader.set("Width", (short) (buffer.getShort() & 0x3fff));
        hwTextureHeader.set("Height", (short) (buffer.getShort() & 0x3fff));
        hwTextureHeader.set("Depth", buffer.getShort());
        hwTextureHeader.set("TotalMipCount", buffer.get());
        hwTextureHeader.set("PixelFormat", pixelFormat.valueOf(buffer.get()));
        hwTextureHeader.set("Unk0", buffer.getShort());
        hwTextureHeader.set("Unk1", buffer.getInt());
        hwTextureHeader.set("UUID", gguuid.read(registry, buffer));

        return hwTextureHeader;
    }

    @NotNull
    public static RTTIObject readTextureData(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final RTTIObject hwTexture = RTTIUtils.newClassBuilder(registry, "HwTexture")
            .member("RemainingDataSize", "uint32")
            .member("InternalDataSize", "uint32")
            .member("ExternalDataSize", "uint32")
            .member("ExternalMipCount", "uint32")
            .member("Unk0", "uint32")
            .member("Unk1", "uint32")
            .build().instantiate();

        hwTexture.set("RemainingDataSize", buffer.getInt());
        hwTexture.set("InternalDataSize", buffer.getInt());
        hwTexture.set("ExternalDataSize", buffer.getInt());
        hwTexture.set("ExternalMipCount", buffer.getInt());
        hwTexture.set("Unk0", buffer.getInt());
        hwTexture.set("Unk1", buffer.getInt());

        if (hwTexture.i32("ExternalDataSize") > 0) {
            final RTTIObject dataSource = RTTIUtils.newClassBuilder(registry, "HwTextureDataSource")
                .member("Location", "String")
                .member("UUID", "GGUUID")
                .member("Channel", "uint32")
                .member("Offset", "uint32")
                .member("Length", "uint32")
                .build().instantiate();

            dataSource.set("Location", IOUtils.getString(buffer, buffer.getInt()));
            dataSource.set("UUID", registry.find("GGUUID").read(registry, buffer));
            dataSource.set("Channel", buffer.getInt());
            dataSource.set("Offset", buffer.getInt());
            dataSource.set("Length", buffer.getInt());

            hwTexture.define("ExternalDataSource", dataSource.getType(), dataSource);
        }

        if (hwTexture.i32("InternalDataSize") > 0) {
            // HACK: InternalDataSize may be greater than the actual size of remaining data
            final Byte[] data = IOUtils.box(IOUtils.getBytesExact(buffer, Math.min(hwTexture.get("InternalDataSize"), buffer.remaining())));
            hwTexture.define("InternalData", registry.find("Array<uint8>"), data);
        }

        return hwTexture;
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
