package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.StorageReadDevice;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

public class ObjectStreamingSystem {
    private final StorageReadDevice device;
    private final RTTIObject graph;

    public ObjectStreamingSystem(@NotNull StorageReadDevice device, @NotNull RTTIObject graph) {
        this.device = device;
        this.graph = graph;
    }

    @NotNull
    public ByteBuffer getDataSourceData(@NotNull RTTIObject locator, int offset, int length) throws IOException {
        assert locator.type().isInstanceOf("StreamingDataSourceLocator");
        final long data = locator.i64("Data");
        return getFileData((int) (data & 0xffffff), (data >>> 24) + offset, length);
    }

    @NotNull
    public ByteBuffer getFileData(int fileId, long offset, long length) throws IOException {
        return getFileData(graph.<String[]>get("Files")[fileId], offset, length);
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
    public StorageReadDevice getDevice() {
        return device;
    }

    @NotNull
    public RTTIObject getGraph() {
        return graph;
    }
}
