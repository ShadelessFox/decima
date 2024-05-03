package com.shade.decima.hfw;

import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class StreamingObjectReader implements RTTIBinaryReader {
    private static final boolean DEBUG = false;

    private final RTTIFactory factory;
    private final ObjectStreamingSystem streamingSystem;
    private final StreamingGraphResource streamingGraph;

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

    public StreamingObjectReader(@NotNull RTTIFactory factory, @NotNull ObjectStreamingSystem streamingSystem) {
        this.factory = factory;
        this.streamingSystem = streamingSystem;
        this.streamingGraph = streamingSystem.getGraph();
    }

    @NotNull
    public ObjectResult readObject(@NotNull String uuid) throws IOException {
        return readObject(RTTIUtils.uuidFromString(factory.find("GGUUID"), uuid));
    }

    @NotNull
    public ObjectResult readObject(@NotNull RTTIObject gguuid) throws IOException {
        final RTTIObject group = Objects.requireNonNull(streamingGraph.getGroup(gguuid), () -> "Group not found: " + RTTIUtils.uuidToString(gguuid));
        final int index = Objects.requireNonNull(streamingGraph.getRootIndex(gguuid), () -> "Root index not found for group: " + RTTIUtils.uuidToString(gguuid));

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
            locatorByDataSource.put(dataSource, streamingGraph.getLocatorTable()[streamingLocatorIndex++]);
        }
    }

    private void resolveLink(@NotNull RTTITypeReference type, @NotNull RTTIReference reference) {
        if (!(reference instanceof RTTIReference.StreamingLink link)) {
            return;
        }

        final ObjectStreamingSystem.LinkReadResult result = streamingSystem.readLink(streamingLinkIndex);
        final int linkGroup = result.group();
        final int linkIndex = result.index();

        streamingLinkIndex = result.position();

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
        final RTTIObject group = Objects.requireNonNull(streamingGraph.getGroup(id), () -> "Group not found: " + id);
        System.out.println("  ".repeat(depth) + "Reading group \033[34m" + id + "\033[0m");

        for (GroupInfo info : groups) {
            if (info.group == group) {
                return info;
            }
        }

        depth++;

        final List<GroupInfo> subGroups = new ArrayList<>();
        for (int i = 0; i < group.i32("SubGroupCount"); i++) {
            subGroups.add(readGroup(streamingGraph.getSubGroups()[group.i32("SubGroupStart") + i], groups));
        }

        final RTTIObject[] objects = new RTTIObject[group.i32("NumObjects")];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new RTTIObject(streamingGraph.getType(group.i32("TypeStart") + i), new LinkedHashMap<>());
        }

        final GroupInfo groupInfo = new GroupInfo(group, objects);

        currentSubGroups = subGroups;
        currentGroup = groupInfo;
        streamingLinkIndex = group.i32("LinkStart");
        streamingLocatorIndex = group.i32("LocatorStart");

        for (int spanIndex = 0, objectIndex = 0; spanIndex < group.i32("SpanCount"); spanIndex++) {
            final RTTIObject span = streamingGraph.getSpanTable()[group.i32("SpanStart") + spanIndex];
            final ByteBuffer buffer = getSpanData(span);

            while (buffer.hasRemaining()) {
                final RTTIClass type = streamingGraph.getType(group.i32("TypeStart") + objectIndex);

                if (DEBUG) {
                    System.out.printf("  ".repeat(depth) + "Reading \033[33m%s\033[0m at \033[34m%#010x\033[0m in \033[33m%s\033[0m%n", type, span.i32("Offset") + buffer.position(), getSpanFile(span));
                }

                objects[objectIndex++].set(read(type, factory, buffer));
            }
        }

        depth--;
        groups.add(groupInfo);
        return groupInfo;
    }

    @NotNull
    private String getSpanFile(@NotNull RTTIObject span) {
        return streamingGraph.getFiles()[span.i32("FileIndexAndIsPatch") & 0x7fffffff];
    }

    @NotNull
    private ByteBuffer getSpanData(@NotNull RTTIObject span) throws IOException {
        return streamingSystem.getFileData(getSpanFile(span), span.i32("Offset"), span.i32("Length"));
    }
}
