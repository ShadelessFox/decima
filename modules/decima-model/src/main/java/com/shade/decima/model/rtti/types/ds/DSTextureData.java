package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.base.BaseTextureData;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class DSTextureData extends BaseTextureData {
    private long unk;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new DSTextureData();
        object.remainingDataSize = buffer.getInt();
        object.internalDataSize = buffer.getInt();
        object.externalDataSize = buffer.getInt();
        object.externalMipCount = buffer.getInt();
        object.unk = buffer.getLong();

        if (object.externalDataSize > 0) {
            object.externalData = DSDataSource.read(registry, buffer);
        }

        if (object.internalDataSize > 0) {
            // HACK: InternalDataSize may be greater than the actual size of remaining data
            object.internalData = BufferUtils.getBytes(buffer, Math.min(object.internalDataSize, buffer.remaining()));
        }

        return new RTTIObject(registry.find(DSTextureData.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        buffer.putInt(remainingDataSize);
        buffer.putInt(internalDataSize);
        buffer.putInt(externalDataSize);
        buffer.putInt(externalMipCount);
        buffer.putLong(unk);

        if (externalDataSize > 0) {
            externalData.<HwDataSource>cast().write(registry, buffer);
        }

        if (internalDataSize > 0) {
            buffer.put(internalData);
        }
    }

    @Override
    public int getSize() {
        int size = 24;

        if (externalDataSize > 0) {
            size += externalData.<HwDataSource>cast().getSize();
        }

        if (internalDataSize > 0) {
            size += internalData.length;
        }

        return size;
    }
}
