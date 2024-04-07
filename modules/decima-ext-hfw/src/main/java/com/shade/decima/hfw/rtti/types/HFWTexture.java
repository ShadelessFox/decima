package com.shade.decima.hfw.rtti.types;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HFWTexture extends HwTexture {
    public HFWTexture(@NotNull RTTIObject header, @NotNull RTTIObject data) {
        super(header, data);
    }

    @NotNull
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final var header = HFWTextureHeader.read(factory, buffer);
        final var data = HFWTextureData.read(factory, buffer);
        return new RTTIObject(factory.find(HFWTexture.class), new HFWTexture(header, data));
    }
}
