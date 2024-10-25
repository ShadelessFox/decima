package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
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
        static VertexStreamElement read(@NotNull ByteBuffer buffer) {
            var offset = buffer.get();
            var storageType = EVertexElementStorageType.valueOf(buffer.get());
            var slotsUsed = buffer.get();
            var type = EVertexElement.valueOf(buffer.get());

            var element = RTTI.newInstance(VertexStreamElement.class);
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
        static VertexStream read(@NotNull ByteBuffer buffer, int numVertices) {
            var flags = buffer.getInt();
            var stride = buffer.getInt();
            var elements = BufferUtils.getStructs(buffer, buffer.getInt(), VertexStreamElement::read);
            var hash = BufferUtils.getBytes(buffer, 16);
            var data = BufferUtils.getBytes(buffer, stride * numVertices);

            var stream = RTTI.newInstance(VertexStream.class);
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
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull VertexArrayData object) {
        var numVertices = buffer.getInt();
        var numStreams = buffer.getInt();
        var streams = BufferUtils.getStructs(buffer, numStreams, buf -> VertexStream.read(buf, numVertices));

        object.count(numVertices);
        object.streams(streams);
    }
}
