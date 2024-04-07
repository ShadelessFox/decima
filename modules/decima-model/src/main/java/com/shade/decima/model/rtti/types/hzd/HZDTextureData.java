package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.base.BaseTextureData;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HZDTextureData extends BaseTextureData {
    @NotNull
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final var object = new HZDTextureData();
        object.remainingDataSize = buffer.getInt();
        object.internalDataSize = buffer.getInt();
        object.externalDataSize = buffer.getInt();
        object.externalMipCount = buffer.getInt();

        if (object.externalDataSize > 0) {
            object.externalData = HZDDataSource.read(factory, buffer);
        }

        if (object.internalDataSize > 0) {
            // HACK: InternalDataSize may be greater than the actual size of remaining data
            object.internalData = BufferUtils.getBytes(buffer, Math.min(object.internalDataSize, buffer.remaining()));
        }

        return new RTTIObject(factory.find(HZDTextureData.class), object);
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        buffer.putInt(remainingDataSize);
        buffer.putInt(internalDataSize);
        buffer.putInt(externalDataSize);
        buffer.putInt(externalMipCount);

        if (externalDataSize > 0) {
            externalData.<HwDataSource>cast().write(factory, buffer);
        }

        if (internalDataSize > 0) {
            buffer.put(internalData);
        }
    }

    @Override
    public int getSize() {
        int size = 16;

        if (externalDataSize > 0) {
            size += externalData.<HwDataSource>cast().getSize();
        }

        if (internalDataSize > 0) {
            size += internalData.length;
        }

        return size;

    }
}
