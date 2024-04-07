package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HwTexture implements HwType {
    @RTTIField(type = @Type(type = HwTextureHeader.class))
    public RTTIObject header;
    @RTTIField(type = @Type(type = HwTextureData.class))
    public RTTIObject data;

    public HwTexture(@NotNull RTTIObject header, @NotNull RTTIObject data) {
        this.header = header;
        this.data = data;
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        header.<HwTextureHeader>cast().write(factory, buffer);
        data.<HwTextureData>cast().write(factory, buffer);
    }

    @Override
    public int getSize() {
        return header.<HwTextureHeader>cast().getSize() + data.<HwTextureData>cast().getSize();
    }
}
