package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.DataSource;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

import static com.shade.decima.rtti.UntilDawn.*;

public class TextureCallback implements ExtraBinaryDataCallback<TextureCallback.TextureInfo> {
    public interface TextureHeader {
        @RTTI.Attr(name = "Type", type = "ETextureType", position = 0, offset = 0)
        ETextureType type();

        void type(ETextureType value);

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
        EPixelFormat pixelFormat();

        void pixelFormat(EPixelFormat value);

        @RTTI.Attr(name = "Unk03", type = "Array<uint8>", position = 5, offset = 0)
        byte[] unk03();

        void unk03(byte[] value);

        @RTTI.Attr(name = "Unk0B", type = "Array<uint8>", position = 6, offset = 0)
        byte[] unk0B();

        void unk0B(byte[] value);

        @NotNull
        static TextureHeader read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var type = ETextureType.valueOf(reader.readByte());
            var width = 1 << reader.readByte();
            var height = 1 << reader.readByte();
            var unk03 = reader.readBytes(7);
            var mips = reader.readByte();
            var pixelFormat = EPixelFormat.valueOf(reader.readByte());
            var unk0B = reader.readBytes(20 + 16);

            var object = factory.newInstance(TextureHeader.class);
            object.type(type);
            object.width(width);
            object.height(height);
            object.mips(mips);
            object.pixelFormat(pixelFormat);
            object.unk03(unk03);
            object.unk0B(unk0B);

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
        static TextureData read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var remainingSize = reader.readInt();
            var start = reader.position();

            var totalSize = reader.readInt();
            var embeddedSize = reader.readInt();
            var streamedSize = reader.readInt();
            assert totalSize == embeddedSize + streamedSize;

            var streamedMips = reader.readInt();
            var embeddedData = reader.readBytes(embeddedSize);
            var streamedData = streamedSize > 0 ? DataSource.read(reader, factory) : null;
            assert reader.position() == start + remainingSize;

            var object = factory.newInstance(TextureData.class);
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
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull TextureInfo object) throws IOException {
        object.header(TextureHeader.read(reader, factory));
        object.data(TextureData.read(reader, factory));
    }
}
