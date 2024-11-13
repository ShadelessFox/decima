package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

import static com.shade.decima.rtti.UntilDawn.*;

public class VertexArrayResourceCallback implements ExtraBinaryDataCallback<VertexArrayResourceCallback.VertexArrayData> {
    public interface VertexStreamElement {
        @RTTI.Attr(name = "Offset", type = "uint8", position = 0, offset = 0)
        byte offset();

        void offset(byte value);

        @RTTI.Attr(name = "StorageType", type = "EVertexElementStorageType", position = 1, offset = 0)
        EVertexElementStorageType storageType();

        void storageType(EVertexElementStorageType value);

        @RTTI.Attr(name = "SlotsUsed", type = "uint8", position = 2, offset = 0)
        byte slotsUsed();

        void slotsUsed(byte value);

        @RTTI.Attr(name = "Type", type = "EVertexElement", position = 3, offset = 0)
        EVertexElement type();

        void type(EVertexElement value);

        @NotNull
        static VertexStreamElement read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var offset = reader.readByte();
            var storageType = EVertexElementStorageType.valueOf(reader.readByte());
            var slotsUsed = reader.readByte();
            var type = EVertexElement.valueOf(reader.readByte());

            var element = factory.newInstance(VertexStreamElement.class);
            element.offset(offset);
            element.storageType(storageType);
            element.slotsUsed(slotsUsed);
            element.type(type);

            return element;
        }
    }

    public interface VertexStream {
        @RTTI.Attr(name = "Flags", type = "uint32", position = 0, offset = 0)
        int flags();

        void flags(int value);

        @RTTI.Attr(name = "Stride", type = "uint32", position = 1, offset = 0)
        int stride();

        void stride(int value);

        @RTTI.Attr(name = "Elements", type = "Array<VertexStreamElement>", position = 2, offset = 0)
        List<VertexStreamElement> elements();

        void elements(List<VertexStreamElement> value);

        @RTTI.Attr(name = "Hash", type = "Array<uint8>", position = 3, offset = 0)
        byte[] hash();

        void hash(byte[] value);

        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 4, offset = 0)
        byte[] data();

        void data(byte[] value);

        @NotNull
        static VertexStream read(@NotNull BinaryReader reader, @NotNull TypeFactory factory, int numVertices) throws IOException {
            var flags = reader.readInt();
            var stride = reader.readInt();
            var elements = reader.readObjects(reader.readInt(), r -> VertexStreamElement.read(r, factory));
            var hash = reader.readBytes(16);
            var data = reader.readBytes(stride * numVertices);

            var stream = factory.newInstance(VertexStream.class);
            stream.flags(flags);
            stream.stride(stride);
            stream.elements(elements);
            stream.hash(hash);
            stream.data(data);

            return stream;
        }
    }

    public interface VertexArrayData {
        @RTTI.Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @RTTI.Attr(name = "Streams", type = "Array<VertexStream>", position = 0, offset = 0)
        List<VertexStream> streams();

        void streams(List<VertexStream> value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull VertexArrayData object) throws IOException {
        var numVertices = reader.readInt();
        var numStreams = reader.readInt();
        var streams = reader.readObjects(numStreams, r -> VertexStream.read(r, factory, numVertices));

        object.count(numVertices);
        object.streams(streams);
    }
}
