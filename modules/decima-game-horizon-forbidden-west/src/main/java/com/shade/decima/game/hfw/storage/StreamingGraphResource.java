package com.shade.decima.game.hfw.storage;

import com.shade.decima.game.hfw.rtti.HFWTypeId;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.runtime.ClassTypeInfo;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.*;

public class StreamingGraphResource {
    private final HorizonForbiddenWest.StreamingGraphResource graph;

    private final List<ClassTypeInfo> types;
    private final Map<GGUUID, StreamingGroupData> groupByUuid = new HashMap<>(); // RootUUID -> Group
    private final Map<Integer, StreamingGroupData> groupById = new HashMap<>(); // GroupId -> Group
    private final Map<GGUUID, Integer> rootIndexByRootUuid = new HashMap<>(); // RootUUIDs -> RootIndices

    public StreamingGraphResource(@NotNull HorizonForbiddenWest.StreamingGraphResource graph, @NotNull TypeFactory factory) throws IOException {
        this.graph = graph;
        this.types = readTypeTable(graph, factory);

        var rootUuids = graph.rootUUIDs();
        var rootIndices = graph.rootIndices();
        var groups = graph.groups();

        for (var group : groups) {
            groupById.put(group.groupID(), group);

            for (var i = group.rootStart(); i < group.rootStart() + group.rootCount(); i++) {
                groupByUuid.put(rootUuids.get(i), group);
                rootIndexByRootUuid.put(rootUuids.get(i), rootIndices[i]);
            }
        }
    }

    public long linkTableID() {
        return graph.linkTableID();
    }

    public int linkTableSize() {
        return graph.linkTableSize();
    }

    @NotNull
    public List<String> files() {
        return graph.files();
    }

    @NotNull
    public List<StreamingSourceSpan> spanTable() {
        return graph.spanTable();
    }

    @NotNull
    public int[] subGroups() {
        return graph.subGroups();
    }

    @NotNull
    public List<ClassTypeInfo> types() {
        return types;
    }

    @NotNull
    public List<StreamingDataSourceLocator> locatorTable() {
        return graph.locatorTable();
    }

    @Nullable
    public StreamingGroupData group(@NotNull GGUUID rootUUID) {
        return groupByUuid.get(rootUUID);
    }

    @Nullable
    public StreamingGroupData group(int groupId) {
        return groupById.get(groupId);
    }

    @Nullable
    public Integer rootIndex(@NotNull GGUUID rootUUID) {
        return rootIndexByRootUuid.get(rootUUID);
    }

    @NotNull
    private static List<ClassTypeInfo> readTypeTable(@NotNull HorizonForbiddenWest.StreamingGraphResource graph, @NotNull TypeFactory factory) throws IOException {
        var reader = BinaryReader.wrap(graph.typeTableData());

        reader.readInt(value -> value == 0, value -> "Unsupported compression: " + value);
        reader.readInt(value -> value == 2, value -> "Unsupported stride: " + value);
        var count = reader.readInt();
        reader.readInt(value -> value == count, value -> "Count mismatch");
        reader.readInt(value -> value == 1, value -> "Unexpected unknown value: " + value);

        var types = new ArrayList<ClassTypeInfo>();

        for (int i = 0; i < count; i++) {
            var index = Short.toUnsignedInt(reader.readShort());
            var hash = graph.typeHashes()[index];
            var type = factory.get(HFWTypeId.of(hash));
            types.add(type);
        }

        return types;
    }
}
