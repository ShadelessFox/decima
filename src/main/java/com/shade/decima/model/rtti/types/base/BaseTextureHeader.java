package com.shade.decima.model.rtti.types.base;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class BaseTextureHeader implements HwTextureHeader {
    @RTTIField(type = @Type(name = "ETextureType"))
    public RTTITypeEnum.Constant type;
    @RTTIField(type = @Type(name = "uint16"))
    public short width;
    @RTTIField(type = @Type(name = "uint16"))
    public short height;
    @RTTIField(type = @Type(name = "uint16"))
    public short depth;
    @RTTIField(type = @Type(name = "uint8"))
    public byte mipCount;
    @RTTIField(type = @Type(name = "EPixelFormat"))
    public RTTITypeEnum.Constant pixelFormat;

    private short unk0;
    private int unk1;
    private byte[] unk2;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new BaseTextureHeader();
        object.type = registry.<RTTITypeEnum>find("ETextureType").valueOf(buffer.getShort());
        object.width = buffer.getShort();
        object.height = buffer.getShort();
        object.depth = buffer.getShort();
        object.mipCount = buffer.get();
        object.pixelFormat = registry.<RTTITypeEnum>find("EPixelFormat").valueOf(buffer.get());
        object.unk0 = buffer.getShort();
        object.unk1 = buffer.getInt();
        object.unk2 = IOUtils.getBytesExact(buffer, 16);

        return new RTTIObject(registry.find(BaseTextureHeader.class), object);
    }

    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        buffer.putShort((short) type.value());
        buffer.putShort(width);
        buffer.putShort(height);
        buffer.putShort(depth);
        buffer.put(mipCount);
        buffer.put((byte) pixelFormat.value());
        buffer.putShort(unk0);
        buffer.putInt(unk1);
        buffer.put(unk2);
    }

    @Override
    public int getSize() {
        return 32;
    }

    @NotNull
    @Override
    public String getType() {
        return type.name();
    }

    @Override
    public int getWidth() {
        return width & 0x3fff;
    }

    @Override
    public int getHeight() {
        return height & 0x3fff;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public int getMipCount() {
        return mipCount;
    }

    @NotNull
    @Override
    public String getPixelFormat() {
        return pixelFormat.name();
    }
}
