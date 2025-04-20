package com.shade.decima.game.hfw.storage;

import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.StreamingDataSource;
import com.shade.util.NotNull;

import java.io.IOException;

public final class ObjectStreamingSystem {
    /**
     * Represents the result of reading a link from the link table.
     *
     * @param position The new position of the link in the link table
     * @param group    The group index
     * @param index    The index of object within the group
     */
    public record LinkReadResult(int position, int group, int index) {
    }

    private final StorageReadDevice device;
    private final StreamingGraphResource graph;
    private final byte[] links;

    public ObjectStreamingSystem(@NotNull StorageReadDevice device, @NotNull StreamingGraphResource graph) throws IOException {
        this.device = device;
        this.graph = graph;
        this.links = getFileData(Math.toIntExact(graph.linkTableID()), 0, graph.linkTableSize());
    }

    @NotNull
    public byte[] getDataSourceData(@NotNull StreamingDataSource dataSource) throws IOException {
        return getDataSourceData(dataSource, dataSource.offset(), dataSource.length());
    }

    @NotNull
    public byte[] getDataSourceData(@NotNull StreamingDataSource dataSource, int offset, int length) throws IOException {
        return getFileData((int) (dataSource.locator() & 0xffffff), Math.addExact(dataSource.locator() >>> 24, offset), length);
    }

    @NotNull
    public byte[] getFileData(int fileId, long offset, long length) throws IOException {
        return getFileData(graph.files().get(fileId), offset, length);
    }

    @NotNull
    public byte[] getFileData(@NotNull String file, long offset, long length) throws IOException {
        var reader = device.resolve(file);
        var buffer = new byte[Math.toIntExact(length)];

        synchronized (reader) {
            reader.position(offset);
            reader.readBytes(buffer, 0, buffer.length);
        }

        return buffer;
    }

    @NotNull
    public StreamingGraphResource graph() {
        return graph;
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
}
