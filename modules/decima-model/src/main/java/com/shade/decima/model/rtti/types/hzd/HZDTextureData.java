package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.base.BaseTextureData;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HZDTextureData extends BaseTextureData {
    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HZDTextureData();
        object.remainingDataSize = buffer.getInt();
        final int position = buffer.position();
        object.internalDataSize = buffer.getInt();
        object.externalDataSize = buffer.getInt();
        object.externalMipCount = buffer.getInt();

        if (object.externalDataSize > 0) {
            // throw new NotImplementedException();
        }

        if (object.internalDataSize > 0) {
            object.internalData = BufferUtils.getBytes(buffer, object.remainingDataSize - 12);
        }

        final int read = buffer.position() - position;
        if (read != object.remainingDataSize) {
            throw new IllegalStateException("Read " + read + " bytes, expected " + object.remainingDataSize);
        }

        return new RTTIObject(registry.find(HZDTextureData.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        buffer.putInt(remainingDataSize);
        buffer.putInt(internalDataSize);
        buffer.putInt(externalDataSize);
        buffer.putInt(externalMipCount);

        if (externalDataSize > 0) {
            externalData.<HwDataSource>cast().write(registry, buffer);
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
