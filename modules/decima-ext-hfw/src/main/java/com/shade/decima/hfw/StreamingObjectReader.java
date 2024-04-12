package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.DirectStorageArchive;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StreamingObjectReader implements RTTIBinaryReader {
    private static final boolean DEBUG = false;

    private final Project project;
    private final RTTIObject graph;
    private final RTTIClass[] types;
    private final byte[] links;
    private final Map<RTTIObject, RTTIObject> groupByUuid = new HashMap<>(); // RootUUID -> Group
    private final Map<Integer, RTTIObject> groupById = new HashMap<>(); // GroupId -> Group
    private final Map<RTTIObject, Integer> rootIndexByRootUuid = new HashMap<>(); // RootUUIDs -> RootIndices

    // State for reading
    private final Map<RTTIObject, RTTIObject> locatorByDataSource = new IdentityHashMap<>(); // StreamingDataSource -> StreamingDataSourceLocator

    private int streamingLinkIndex;
    private int streamingLocatorIndex;
    private int depth;

    private GroupInfo currentGroup;
    private List<GroupInfo> currentSubGroups;

    public record GroupInfo(@NotNull RTTIObject group, @NotNull RTTIObject[] objects) {
    }

    public record GroupResult(
        @NotNull List<GroupInfo> groups,
        @NotNull Map<RTTIObject, RTTIObject> locators
    ) {
        @NotNull
        public GroupInfo root() {
            return groups.get(groups.size() - 1);
        }

        @Override
        public String toString() {
            return "Result[groups=" + groups.size() + ", locators=" + locators.size() + "]";
        }
    }

    public record ObjectResult(
        @NotNull GroupResult groupResult,
        @NotNull RTTIObject object
    ) {}

    public StreamingObjectReader(@NotNull Project project) throws IOException {
        this.project = project;
        this.graph = readGraph();
        this.types = readTypeTable();
        this.links = readStreamingLinks();

        final var rootUuids = graph.objs("RootUUIDs");
        final var rootIndices = graph.ints("RootIndices");
        final var groups = graph.objs("Groups");

        for (RTTIObject group : groups) {
            groupById.put(group.i32("GroupID"), group);

            for (int i = group.i32("RootStart"); i < group.i32("RootStart") + group.i32("RootCount"); i++) {
                groupByUuid.put(rootUuids[i], group);
                rootIndexByRootUuid.put(rootUuids[i], rootIndices[i]);
            }
        }
    }

    @NotNull
    public ObjectResult readObject(@NotNull String uuid) throws IOException {
        final RTTIObject gguuid = RTTIUtils.uuidFromString(project.getRTTIFactory().find("GGUUID"), uuid);
        final RTTIObject group = Objects.requireNonNull(groupByUuid.get(gguuid), () -> "Group not found: " + uuid);
        final int index = Objects.requireNonNull(rootIndexByRootUuid.get(gguuid), () -> "Root index not found: " + uuid);

        final GroupResult result = readGroup(group.i32("GroupID"));
        final RTTIObject object = result.root().objects()[index];

        return new ObjectResult(result, object);
    }

    @NotNull
    public GroupResult readGroup(int id) throws IOException {
        final List<GroupInfo> groups = new ArrayList<>();
        readGroup(id, groups);

        final Map<RTTIObject, RTTIObject> locators = new IdentityHashMap<>(locatorByDataSource);
        locatorByDataSource.clear();

        return new GroupResult(groups, locators);
    }

    @NotNull
    @Override
    public <T> T read(@NotNull RTTIType<T> type, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final T result = type.read(factory, this, buffer);

        if (type instanceof RTTITypeReference r) {
            resolveLink(r, (RTTIReference) result);
        } else if (type.getTypeName().equals("StreamingDataSource")) {
            resolveStreamingDataSource((RTTIObject) result);
        }

        return result;
    }

    private void resolveStreamingDataSource(@NotNull RTTIObject dataSource) {
        if (dataSource.i8("Channel") != -1 && dataSource.i32("Length") > 0) {
            locatorByDataSource.put(dataSource, graph.objs("LocatorTable")[streamingLocatorIndex++]);
        }
    }

    private void resolveLink(@NotNull RTTITypeReference type, @NotNull RTTIReference reference) {
        if (!(reference instanceof RTTIReference.StreamingLink link)) {
            return;
        }

        int v7 = links[streamingLinkIndex++];

        int linkIndex = v7 & 0x3f;
        if ((v7 & 0x80) != 0) {
            byte v10;
            do {
                v10 = links[streamingLinkIndex++];
                linkIndex = (linkIndex << 7) | (v10 & 0x7f);
            } while ((v10 & 0x80) != 0);
        }

        var linkGroup = -1;
        if ((v7 & 0x40) != 0) {
            linkGroup = linkIndex;
            var v14 = links[streamingLinkIndex++];
            linkIndex = v14 & 0x7f;
            if ((v14 & 0x80) != 0) {
                byte v16;
                do {
                    v16 = links[streamingLinkIndex++];
                    linkIndex = (linkIndex << 7) | (v16 & 0x7f);
                } while ((v16 & 0x80) != 0);
            }
        }

        if (type.getTypeName().equals("StreamingRef")) {
            // Can't resolve streaming references without actually playing the game
            return;
        }

        final GroupInfo group;
        if (linkGroup == -1) {
            // References the current group being read
            group = currentGroup;
        } else {
            // Seems to reference subgroups
            group = currentSubGroups.get(linkGroup);
        }

        final RTTIObject object = group.objects[linkIndex];
        final RTTIClass actualType = object.type();
        final boolean matches = actualType.isInstanceOf(type.getComponentType());

        if (DEBUG) {
            System.out.println("  ".repeat(depth) + "Resolving \033[33m" + type + "\033[0m to an object at index "
                + "\033[34m" + linkIndex + "\033[0m, group index: \033[34m" + linkGroup + "\033[0m, in group \033[34m" + group.group.i32("GroupID") + "\033[0m"
                + " (object: \033[33m" + actualType + "\033[0m"
                + ", matches: " + (matches ? "\033[32mtrue\033[0m" : "\033[31mfalse\033[0m") + ")");
        }

        if (!matches) {
            throw new NotImplementedException();
        }

        link.setTarget(object);
    }

    @NotNull
    private GroupInfo readGroup(int id, @NotNull List<GroupInfo> groups) throws IOException {
        final RTTIObject group = Objects.requireNonNull(groupById.get(id), () -> "Group not found: " + id);
        System.out.println("  ".repeat(depth) + "Reading group \033[34m" + id + "\033[0m");

        for (GroupInfo info : groups) {
            if (info.group == group) {
                return info;
            }
        }

        depth++;

        final List<GroupInfo> subGroups = new ArrayList<>();
        for (int i = 0; i < group.i32("SubGroupCount"); i++) {
            subGroups.add(readGroup(graph.ints("SubGroups")[group.i32("SubGroupStart") + i], groups));
        }

        final RTTIObject[] objects = new RTTIObject[group.i32("NumObjects")];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new RTTIObject(types[group.i32("TypeStart") + i], new LinkedHashMap<>());
        }

        final GroupInfo groupInfo = new GroupInfo(group, objects);

        currentSubGroups = subGroups;
        currentGroup = groupInfo;
        streamingLinkIndex = group.i32("LinkStart");
        streamingLocatorIndex = group.i32("LocatorStart");

        for (int spanIndex = 0, objectIndex = 0; spanIndex < group.i32("SpanCount"); spanIndex++) {
            final RTTIObject span = graph.objs("SpanTable")[group.i32("SpanStart") + spanIndex];
            final ByteBuffer buffer = getSpanData(span);

            while (buffer.hasRemaining()) {
                final RTTIClass type = types[group.i32("TypeStart") + objectIndex];

                if (DEBUG) {
                    System.out.printf("  ".repeat(depth) + "Reading \033[33m%s\033[0m at \033[34m%#010x\033[0m in \033[33m%s\033[0m%n", type, span.i32("Offset") + buffer.position(), getSpanFile(span));
                }

                objects[objectIndex++].set(read(type, project.getRTTIFactory(), buffer));
            }
        }

        depth--;
        groups.add(groupInfo);
        return groupInfo;
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
    public ByteBuffer getStreamingData(
        @NotNull RTTIObject streamingDataSourceLocator,
        @NotNull RTTIObject streamingDataSource
    ) throws IOException {
        var offset = streamingDataSource.i32("Offset");
        var length = streamingDataSource.i32("Length");

        return getStreamingData(streamingDataSourceLocator, offset, length);
    }

    @NotNull
    public ByteBuffer getStreamingData(
        @NotNull RTTIObject streamingDataSourceLocator,
        int offset,
        int length
    ) throws IOException {
        var data = streamingDataSourceLocator.i64("Data");
        var path = graph.<String[]>get("Files")[(int) (data & 0xffffff)];

        return getArchiveData(path, offset + (data >>> 24), length);
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
    private byte[] readStreamingLinks() throws IOException {
        return Files.readAllBytes(resolveFilePath(Math.toIntExact(graph.i64("LinkTableID"))));
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
