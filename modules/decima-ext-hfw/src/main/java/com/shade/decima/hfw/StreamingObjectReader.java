package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.DirectStorageArchive;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StreamingObjectReader {
    private final Project project;
    private final RTTIObject graph;
    private final RTTIClass[] types;
    private final ByteBuffer links;

    private final Map<RTTIObject, RTTIObject> groupByUuid = new HashMap<>(); // RootUUID -> Group
    private final Map<Integer, RTTIObject> groupById = new HashMap<>(); // GroupId -> Group

    public StreamingObjectReader(@NotNull Project project) throws IOException {
        this.project = project;
        this.graph = readGraph();
        this.types = readTypeTable();
        this.links = readStreamingLinks();

        final RTTIObject[] roots = graph.objs("RootUUIDs");
        final RTTIObject[] groups = graph.objs("Groups");
        for (int i = 0; i < roots.length; i++) {
            final RTTIObject group = groups[i];
            for (int j = group.i32("RootStart"); j < group.i32("RootCount"); j++) {
                groupByUuid.put(roots[j], group);
            }
            groupById.put(group.i32("GroupID"), group);
        }
    }

    @NotNull
    public List<RTTIObject> readGroup(int id) throws IOException {
        final List<RTTIObject> objects = new ArrayList<>();
        readGroup(id, 0, objects);
        return objects;
    }

    private void readGroup(int id, int depth, @NotNull List<RTTIObject> objects) throws IOException {
        final RTTIObject group = Objects.requireNonNull(groupById.get(id), () -> "Group not found: " + id);
        System.out.println("  ".repeat(depth) + "Reading group " + id);

        for (int i = group.i32("SubGroupStart"); i < group.i32("SubGroupStart") + group.i32("SubGroupCount"); i++) {
            readGroup(graph.ints("SubGroups")[i], depth + 1, objects);
        }

        for (int i = group.i32("SpanStart"), j = 0; i < group.i32("SpanStart") + group.i32("SpanCount"); i++) {
            final RTTIObject span = graph.objs("SpanTable")[i];
            final ByteBuffer buffer = getSpanData(span);

            while (buffer.hasRemaining()) {
                final RTTIClass type = types[group.i32("TypeStart") + j++];
                System.out.printf("  ".repeat(depth) + "- Reading %s at %d in %s%n", type, span.i32("Offset") + buffer.position(), getSpanFile(span));
                objects.add(type.read(project.getRTTIFactory(), buffer));
            }
        }

        links.position(group.i32("LinkStart"));
        for (int i = 0; i < group.i32("LinkSize"); i++) {
            int v7 = links.get();
            int v8 = v7 & 0x3f;
            if ((v7 & 0x80) != 0) {
                byte v10;
                do {
                    v10 = links.get();
                    v8 = (v8 << 7) | (v10 & 0x7f);
                } while ((v10 & 0x80) != 0);
            }
            var v11 = -1;
            if ((v7 & 0x40) != 0) {
                v11 = v8;
                var v14 = links.get();
                v8 = v14 & 0x7f;
                if ((v14 & 0x80) != 0) {
                    byte v16;
                    do {
                        v16 = links.get();
                        v8 = (v8 << 7) | (v16 & 0x7f);
                    } while ((v16 & 0x80) != 0);
                }
            }

            System.out.println("  ".repeat(depth) + "- Local link (index1: " + v8 + ", index2: " + v11 + ")");
        }
    }

    @NotNull
    private String getSpanFile(@NotNull RTTIObject span) {
        return graph.<String[]>get("Files")[span.i32("FileIndexAndIsPatch") & 0x7fffffff];
    }

    @NotNull
    private ByteBuffer getSpanData(@NotNull RTTIObject span) throws IOException {
        return getArchiveData(getSpanFile(span), span.i32("Offset"), span.i32("Length"));
    }

    @NotNull
    private ByteBuffer getStreamingData(
        @NotNull RTTIObject streamingDataSource,
        @NotNull RTTIObject streamingDataSourceLocator
    ) throws IOException {
        var data = streamingDataSourceLocator.i64("Data");
        var path = graph.<String[]>get("Files")[(int) (data & 0xffffff)];
        var offset = streamingDataSource.i32("Offset") + (data >>> 24);
        var length = streamingDataSource.i32("Length");

        return getArchiveData(path, offset, length);
    }

    @NotNull
    private ByteBuffer getArchiveData(@NotNull String path, long offset, long length) throws IOException {
        try (DirectStorageArchive archive = new DirectStorageArchive(resolveCachePath(path))) {
            try (InputStream is = archive.newInputStream(offset, length)) {
                return ByteBuffer.wrap(is.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            }
        }
    }

    @NotNull
    private RTTIObject readGraph() throws IOException {
        try (InputStream is = Files.newInputStream(resolveCachePath("cache:package/streaming_graph.core"))) {
            return project.getCoreFileReader().read(is, true).objects().get(0);
        }
    }

    @NotNull
    private RTTIClass[] readTypeTable() {
        final ByteBuffer buffer = ByteBuffer.wrap(graph.get("TypeTableData")).order(ByteOrder.LITTLE_ENDIAN);

        final var compression = BufferUtils.expectInt(buffer, 0, "Unsupported compression");
        final var stride = BufferUtils.expectInt(buffer, 2, "Unsupported stride");
        final var count0 = buffer.getInt();
        final var count1 = BufferUtils.expectInt(buffer, count0, "Count mismatch");
        final var unk = BufferUtils.expectInt(buffer, 1, "Unknown value");
        final var start = buffer.position();

        final long[] hashes = graph.get("TypeHashes");
        final RTTIClass[] types = new RTTIClass[count0];

        for (int i = 0; i < count0; i++) {
            final int index = buffer.getShort(start + i * stride) & 0xffff;
            final long hash = hashes[index];
            final RTTIType<?> type = project.getRTTIFactory().find(hash);

            if (type == null) {
                throw new IllegalStateException("Can't resolve type: %#018x (%s)%n".formatted(hash, Long.toUnsignedString(hash)));
            }

            types[i] = (RTTIClass) type;
        }

        return types;
    }

    @NotNull
    private ByteBuffer readStreamingLinks() throws IOException {
        return ByteBuffer
            .wrap(Files.readAllBytes(resolveFilePath(Math.toIntExact(graph.i64("LinkTableID")))))
            .order(ByteOrder.LITTLE_ENDIAN);
    }

    @NotNull
    private Path resolveFilePath(int index) {
        return resolveCachePath(graph.<String[]>get("Files")[index]);
    }

    @NotNull
    private Path resolveCachePath(@NotNull String path) {
        if (!path.startsWith("cache:")) {
            throw new IllegalArgumentException("Invalid cache path: " + path);
        }
        return project.getContainer().getPackfilesPath().resolve("LocalCacheWinGame").resolve(path.substring(6));
    }
}
