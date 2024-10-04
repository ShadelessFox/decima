package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.data.DataSource;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

import static com.shade.decima.rtti.UntilDawn.*;

public class TextureCallback implements ExtraBinaryDataCallback<TextureCallback.TextureInfo> {
    public interface TextureHeader {
        @RTTI.Attr(name = "Type", type = "ETextureType", position = 0, offset = 0)
        TextureType type();

        void type(TextureType value);

        @RTTI.Attr(name = "Width", type = "uint32", position = 1, offset = 0)
        int width();

        void width(int value);

        @RTTI.Attr(name = "Height", type = "uint32", position = 2, offset = 0)
        int height();

        void height(int value);

        @RTTI.Attr(name = "Mips", type = "uint32", position = 3, offset = 0)
        int mips();

        void mips(int value);

        @RTTI.Attr(name = "PixelFormat", type = "EPixelFormat", position = 4, offset = 0)
        PixelFormat pixelFormat();

        void pixelFormat(PixelFormat value);

        @NotNull
        static TextureHeader read(@NotNull ByteBuffer buffer) {
            var type = TextureType.valueOf(buffer.get());
            var width = 1 << buffer.get();
            var height = 1 << buffer.get();
            buffer.position(buffer.position() + 7);
            var mips = buffer.get();
            var pixelFormat = PixelFormat.valueOf(buffer.get());
            buffer.position(buffer.position() + 20 + 16);

            var object = RTTI.newInstance(TextureHeader.class);
            object.type(type);
            object.width(width);
            object.height(height);
            object.mips(mips);
            object.pixelFormat(pixelFormat);

            return object;
        }
    }

    public interface TextureData {
        @RTTI.Attr(name = "TotalSize", type = "uint32", position = 0, offset = 0)
        int totalSize();

        void totalSize(int value);

        @RTTI.Attr(name = "EmbeddedSize", type = "uint32", position = 1, offset = 0)
        int embeddedSize();

        void embeddedSize(int value);

        @RTTI.Attr(name = "StreamedSize", type = "uint32", position = 2, offset = 0)
        int streamedSize();

        void streamedSize(int value);

        @RTTI.Attr(name = "StreamedMips", type = "uint32", position = 3, offset = 0)
        int streamedMips();

        void streamedMips(int value);

        @RTTI.Attr(name = "EmbeddedData", type = "Array<uint8>", position = 4, offset = 0)
        byte[] embeddedData();

        void embeddedData(byte[] value);

        @RTTI.Attr(name = "StreamedData", type = "DataSource", position = 5, offset = 0)
        DataSource streamedData();

        void streamedData(DataSource value);

        @NotNull
        static TextureData read(@NotNull ByteBuffer buffer) {
            var remainingSize = buffer.getInt();
            var start = buffer.position();

            var totalSize = buffer.getInt();
            var embeddedSize = buffer.getInt();
            var streamedSize = buffer.getInt();
            assert totalSize == embeddedSize + streamedSize;

            var streamedMips = buffer.getInt();
            var embeddedData = BufferUtils.getBytes(buffer, embeddedSize);
            var streamedData = streamedSize > 0 ? DataSource.read(buffer) : null;
            assert buffer.position() == start + remainingSize;

            var object = RTTI.newInstance(TextureData.class);
            object.totalSize(totalSize);
            object.embeddedSize(embeddedSize);
            object.streamedSize(streamedSize);
            object.streamedMips(streamedMips);
            object.embeddedData(embeddedData);
            object.streamedData(streamedData);

            return object;
        }
    }

    public interface TextureInfo {
        @RTTI.Attr(name = "Header", type = "TextureHeader", position = 0, offset = 0)
        TextureHeader header();

        void header(TextureHeader value);

        @RTTI.Attr(name = "Data", type = "TextureData", position = 1, offset = 0)
        TextureData data();

        void data(TextureData value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull TextureInfo object) {
        object.header(TextureHeader.read(buffer));
        object.data(TextureData.read(buffer));
    }
}
