package com.shade.decima.model.rtti.types.hfw;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.base.BaseTextureData;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HFWTextureData extends BaseTextureData {
    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HFWTextureData();
        object.remainingDataSize = buffer.getInt();
        final int position = buffer.position();
        object.internalDataSize = buffer.getInt();
        object.externalDataSize = buffer.getInt();
        object.externalMipCount = buffer.getInt();

        if (object.internalDataSize > 0) {
            object.internalData = BufferUtils.getBytes(buffer, object.remainingDataSize - 12);
        }

        final int read = buffer.position() - position;
        if (read != object.remainingDataSize) {
            throw new IllegalStateException("Read " + read + " bytes, expected " + object.remainingDataSize);
        }

        return new RTTIObject(registry.find(HFWTextureData.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize() {
        throw new NotImplementedException();
    }
}
