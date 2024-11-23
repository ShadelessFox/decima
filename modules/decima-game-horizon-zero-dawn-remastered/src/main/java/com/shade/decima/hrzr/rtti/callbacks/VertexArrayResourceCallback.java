package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.*;

public class VertexArrayResourceCallback implements ExtraBinaryDataCallback<VertexArrayResourceCallback.VertexArrayData> {
    public interface VertexArrayData {
        @Attr(name = "Count", type = "uint32", position = 0, offset = 0)
        int count();

        void count(int value);

        @Attr(name = "IsStreaming", type = "bool", position = 0, offset = 0)
        boolean streaming();

        void streaming(boolean value);

        @Attr(name = "Streams", type = "Array<VertexStream>", position = 0, offset = 0)
        List<VertexStream> streams();

        void streams(List<VertexStream> value);
    }

    public interface VertexStream {
        @Attr(name = "Flags", type = "uint32", position = 0, offset = 0)
        int flags();

        void flags(int value);

        @Attr(name = "Stride", type = "uint32", position = 1, offset = 0)
        int stride();

        void stride(int value);

        @Attr(name = "Elements", type = "Array<VertexStreamElement>", position = 2, offset = 0)
        List<VertexStreamElement> elements();

        void elements(List<VertexStreamElement> value);

        @Attr(name = "Hash", type = "Array<uint8>", position = 3, offset = 0)
        byte[] hash();

        void hash(byte[] value);

        @Attr(name = "Data", type = "Array<uint8>", position = 4, offset = 0)
        byte[] data();

        void data(byte[] value);

        @NotNull
        static VertexStream read(@NotNull BinaryReader reader, @NotNull TypeFactory factory, int numVertices, boolean streaming) throws IOException {
            var flags = reader.readInt();
            var stride = reader.readInt();
            var elements = reader.readObjects(reader.readInt(), r -> VertexStreamElement.read(r, factory));
            var hash = reader.readBytes(16);
            var data = streaming ? null : reader.readBytes(stride * numVertices);

            var stream = factory.newInstance(VertexStream.class);
            stream.flags(flags);
            stream.stride(stride);
            stream.elements(elements);
            stream.hash(hash);
            stream.data(data);

            return stream;
        }
    }

    public interface VertexStreamElement {
        @Attr(name = "Offset", type = "uint8", position = 0, offset = 0)
        byte offset();

        void offset(byte value);

        @Attr(name = "StorageType", type = "ESRTElementFormat", position = 1, offset = 0)
        ESRTElementFormat storageType();

        void storageType(ESRTElementFormat value);

        @Attr(name = "SlotsUsed", type = "uint8", position = 2, offset = 0)
        byte slotsUsed();

        void slotsUsed(byte value);

        @Attr(name = "Type", type = "EVertexElement", position = 3, offset = 0)
        EVertexElement type();

        void type(EVertexElement value);

        @NotNull
        static VertexStreamElement read(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
            var offset = reader.readByte();
            var storageType = ESRTElementFormat.valueOf(reader.readByte());
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

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull VertexArrayData object) throws IOException {
        var numVertices = reader.readInt();
        var numStreams = reader.readInt();
        var streaming = reader.readByteBoolean();
        var streams = reader.readObjects(numStreams, r -> VertexStream.read(r, factory, numVertices, streaming));

        object.count(numVertices);
        object.streams(streams);
        object.streaming(streaming);
    }
}
