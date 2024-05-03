package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.StorageReadDevice;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

public final class ObjectStreamingSystem {
    private final StorageReadDevice device;
    private final StreamingGraphResource graph;
    private final byte[] links;

    public ObjectStreamingSystem(@NotNull StorageReadDevice device, @NotNull StreamingGraphResource graph) throws IOException {
        this.device = device;
        this.graph = graph;
        this.links = getFileData(Math.toIntExact(graph.getLinkTableID()), 0, graph.getLinkTableSize()).array();
    }

    @NotNull
    public ByteBuffer getDataSourceData(@NotNull RTTIObject locator, int offset, int length) throws IOException {
        assert locator.type().isInstanceOf("StreamingDataSourceLocator");
        final long data = locator.i64("Data");
        return getFileData((int) (data & 0xffffff), (data >>> 24) + offset, length);
    }

    @NotNull
    public ByteBuffer getFileData(int fileId, long offset, long length) throws IOException {
        return getFileData(graph.getFiles()[fileId], offset, length);
    }

    @NotNull
    public ByteBuffer getFileData(@NotNull String file, long offset, long length) throws IOException {
        final SeekableByteChannel channel = device.resolve(file);
        final ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(length)).order(ByteOrder.LITTLE_ENDIAN);

        synchronized (channel) {
            channel.position(offset);
            channel.read(buffer);
        }

        if (buffer.hasRemaining()) {
            throw new IOException("Unexpected end of stream");
        }

        return buffer.flip();
    }

    @NotNull
    public LinkReadResult readLink(int position) {
        int v7 = links[position++];

        int linkIndex = v7 & 0x3f;
        if ((v7 & 0x80) != 0) {
            byte v10;
            do {
                v10 = links[position++];
                linkIndex = (linkIndex << 7) | (v10 & 0x7f);
            } while ((v10 & 0x80) != 0);
        }

        var linkGroup = -1;
        if ((v7 & 0x40) != 0) {
            linkGroup = linkIndex;
            var v14 = links[position++];
            linkIndex = v14 & 0x7f;
            if ((v14 & 0x80) != 0) {
                byte v16;
                do {
                    v16 = links[position++];
                    linkIndex = (linkIndex << 7) | (v16 & 0x7f);
                } while ((v16 & 0x80) != 0);
            }
        }

        return new LinkReadResult(position, linkGroup, linkIndex);
    }

    @NotNull
    public StreamingGraphResource getGraph() {
        return graph;
    }

    /**
     * Represents the result of reading a link from the link table.
     *
     * @param position The new position of the link in the link table
     * @param group    The group index
     * @param index    The index of object within the group
     */
    public record LinkReadResult(int position, int group, int index) {}
}
