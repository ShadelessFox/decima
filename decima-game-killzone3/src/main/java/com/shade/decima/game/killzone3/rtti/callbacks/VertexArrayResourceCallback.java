package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class VertexArrayResourceCallback implements ExtraBinaryDataCallback<VertexArrayResourceCallback.VertexArrayData> {
    public interface VertexArrayData {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "Streams", type = "Array<VertexStream>", position = 1, offset = 0)
        List<VertexStream> streams();

        void streams(List<VertexStream> value);

        @Attr(name = "Data", type = "Array<uint8>", position = 2, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    public interface VertexStream {
        @Attr(name = "Flags", type = "uint32", position = 0, offset = 0)
        int flags(); // EResourceCreationFlags

        void flags(int value);

        @Attr(name = "Stride", type = "uint32", position = 1, offset = 0)
        int stride();

        void stride(int value);

        @Attr(name = "Elements", type = "Array<VertexStreamElement>", position = 2, offset = 0)
        List<VertexStreamElement> elements();

        void elements(List<VertexStreamElement> value);

        static VertexStream read(BinaryReader reader, TypeFactory factory) throws IOException {
            var stream = factory.newInstance(VertexStream.class);
            stream.flags(reader.readInt());
            stream.stride(reader.readInt());
            stream.elements(reader.readObjects(reader.readInt(), r -> VertexStreamElement.read(r, factory)));
            return stream;
        }
    }

    public interface VertexStreamElement {
        @Attr(name = "Unk00", type = "uint32", position = 0, offset = 0)
        int unk00();

        void unk00(int value);

        @Attr(name = "Offset", type = "uint8", position = 1, offset = 0)
        byte offset();

        void offset(byte value);

        @Attr(name = "StorageType", type = "uint8", position = 2, offset = 0)
        byte storageType();

        void storageType(byte value);

        @Attr(name = "ComponentCount", type = "uint8", position = 3, offset = 0)
        byte componentCount();

        void componentCount(byte value);

        @Attr(name = "Type", type = "uint8", position = 4, offset = 0)
        byte type();

        void type(byte value);

        static VertexStreamElement read(BinaryReader reader, TypeFactory factory) throws IOException {
            var field = factory.newInstance(VertexStreamElement.class);
            field.unk00(reader.readInt());
            field.offset(reader.readByte());
            field.storageType(reader.readByte());
            field.componentCount(reader.readByte());
            field.type(reader.readByte());
            return field;
        }
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, VertexArrayData object) throws IOException {
        int numVertices = reader.readInt();
        var numStreams = reader.readInt();

        var streams = reader.readObjects(numStreams, r -> VertexStream.read(r, factory));
        var data = reader.readBytes(streams.stream().mapToInt(x -> x.stride() * numVertices).sum());

        object.count(numVertices);
        object.streams(streams);
        object.data(data);
    }
}
