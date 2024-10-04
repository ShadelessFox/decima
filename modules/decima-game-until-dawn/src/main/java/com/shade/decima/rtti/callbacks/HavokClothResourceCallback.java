package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HavokClothResourceCallback implements ExtraBinaryDataCallback<HavokClothResourceCallback.HavokData> {
    public interface HavokData {
        @RTTI.Attr(name = "HavokData", type = "Array<uint8>", position = 0, offset = 0)
        byte[] havokData();

        void havokData(byte[] value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull HavokData object) {
        object.havokData(BufferUtils.getBytes(buffer, buffer.getInt()));
    }
}
