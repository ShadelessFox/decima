package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.EPixelFormat;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.ETextureType;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class UITextureFramesCallback implements ExtraBinaryDataCallback<UITextureFramesCallback.FramesData> {
    public interface FramesData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);

        @Attr(name = "Spans", type = "Array<uint64>", position = 1, offset = 0)
        long[] spans();

        void spans(long[] value);

        @Attr(name = "Width", type = "uint32", position = 2, offset = 0)
        int width();

        void width(int value);

        @Attr(name = "Height", type = "uint32", position = 3, offset = 0)
        int height();

        void height(int value);

        @Attr(name = "Format", type = "EPixelFormat", position = 4, offset = 0)
        EPixelFormat format();

        void format(EPixelFormat value);

        @Attr(name = "Type", type = "ETextureType", position = 5, offset = 0)
        ETextureType type();

        void type(ETextureType value);

        /** Allocation size; dimensions are aligned for compressed textures */
        @Attr(name = "Size", type = "uint32", position = 6, offset = 0)
        int size();

        void size(int value);

        @Attr(name = "Unk01", type = "float", position = 7, offset = 0)
        float unk01();

        void unk01(float value);

        @Attr(name = "Unk02", type = "float", position = 8, offset = 0)
        float unk02();

        void unk02(float value);

        @NotNull
        static FramesData read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var data = factory.newInstance(FramesData.class);
            new UITextureFramesCallback().deserialize(reader, factory, data);
            return data;
        }
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull FramesData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
        object.spans(reader.readLongs(reader.readInt()));
        object.width(reader.readInt());
        object.height(reader.readInt());
        object.format(EPixelFormat.valueOf(reader.readInt()));
        object.type(ETextureType.valueOf(reader.readInt()));
        object.size(reader.readInt());
        object.unk01(reader.readFloat());
        object.unk02(reader.readFloat());
    }
}
