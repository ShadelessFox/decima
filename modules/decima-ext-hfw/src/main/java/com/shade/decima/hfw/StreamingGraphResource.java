package com.shade.decima.hfw;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class StreamingGraphResource {
    private final RTTIObject graph;
    private final RTTIClass[] types;

    private final Map<RTTIObject, RTTIObject> groupByUuid = new HashMap<>(); // RootUUID -> Group
    private final Map<Integer, RTTIObject> groupById = new HashMap<>(); // GroupId -> Group
    private final Map<RTTIObject, Integer> rootIndexByRootUuid = new HashMap<>(); // RootUUIDs -> RootIndices

    public StreamingGraphResource(@NotNull RTTIObject graph, @NotNull RTTIFactory factory) {
        this.graph = graph;
        this.types = readTypeTable(graph, factory);

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

    public long getLinkTableID() {
        return graph.i64("LinkTableID");
    }

    public int getLinkTableSize() {
        return graph.i32("LinkTableSize");
    }

    @NotNull
    public String[] getFiles() {
        return graph.get("Files");
    }

    @NotNull
    public RTTIObject[] getLocatorTable() {
        return graph.get("LocatorTable");
    }

    @NotNull
    public RTTIObject[] getSpanTable() {
        return graph.get("SpanTable");
    }

    @NotNull
    public RTTIObject[] getRootUUIDs() {
        return graph.get("RootUUIDs");
    }

    @NotNull
    public int[] getSubGroups() {
        return graph.get("SubGroups");
    }

    @Nullable
    public RTTIObject getGroup(@NotNull RTTIObject uuid) {
        return groupByUuid.get(uuid);
    }

    @Nullable
    public RTTIObject getGroup(int id) {
        return groupById.get(id);
    }

    @Nullable
    public Integer getRootIndex(@NotNull RTTIObject uuid) {
        return rootIndexByRootUuid.get(uuid);
    }

    @NotNull
    public RTTIClass getType(int index) {
        return types[index];
    }

    @NotNull
    private static RTTIClass[] readTypeTable(@NotNull RTTIObject graph, @NotNull RTTIFactory factory) {
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
            final RTTIType<?> type = factory.find(hash);

            if (type == null) {
                throw new IllegalStateException("Can't resolve type: %#018x (%s)%n".formatted(hash, Long.toUnsignedString(hash)));
            }

            types[i] = (RTTIClass) type;
        }

        return types;
    }
}
