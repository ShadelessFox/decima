package com.shade.decima.model.rtti.types.hfw;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HFWTexture extends HwTexture {
    public HFWTexture(@NotNull RTTIObject header, @NotNull RTTIObject data) {
        super(header, data);
    }

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var header = HFWTextureHeader.read(registry, buffer);
        final var data = HFWTextureData.read(registry, buffer);
        return new RTTIObject(registry.find(HFWTexture.class), new HFWTexture(header, data));
    }
}
