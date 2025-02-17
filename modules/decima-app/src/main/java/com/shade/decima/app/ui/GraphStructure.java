package com.shade.decima.app.ui;

import com.shade.decima.app.ui.tree.TreeStructure;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.StreamingGroupData;
import com.shade.decima.game.hfw.storage.StreamingGraphResource;
import com.shade.decima.rtti.runtime.ClassTypeInfo;
import com.shade.util.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

record GraphStructure(
    @NotNull StreamingGraphResource graph
) implements TreeStructure<GraphStructure.Element> {
    public sealed interface Element {
        @NotNull
        StreamingGraphResource graph();

        record Root(@NotNull StreamingGraphResource graph) implements Element {
            @Override
            public String toString() {
                return "Graph";
            }
        }

        record Group(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group
        ) implements Element, Comparable<Element.Group> {
            @Override
            public String toString() {
                return "Group " + group.groupID();
            }

            @Override
            public int compareTo(Group o) {
                return Integer.compare(group.groupID(), o.group().groupID());
            }
        }

        record GroupDependencyGroups(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group
        ) implements Element {
            @Override
            public String toString() {
                return "Dependencies (" + group.subGroupCount() + ")";
            }
        }

        record GroupDependentGroups(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group
        ) implements Element {
            @Override
            public String toString() {
                return "Dependents (" + graph.incomingGroups(group).size() + ")";
            }
        }

        record GroupRoots(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group
        ) implements Element {
            @Override
            public String toString() {
                return "Roots (" + group.rootCount() + ")";
            }
        }

        record GroupObjects(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group,
            @NotNull Set<Options> options
        ) implements Element {
            public enum Options {
                GROUP_BY_TYPE,
                SORT_BY_COUNT
            }

            public GroupObjects(@NotNull StreamingGraphResource graph, @NotNull StreamingGroupData group) {
                this(graph, group, EnumSet.noneOf(Options.class));
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof GroupObjects that
                    && Objects.equals(group, that.group)
                    && Objects.equals(graph, that.graph);
            }

            @Override
            public int hashCode() {
                return Objects.hash(graph, group);
            }

            @Override
            public String toString() {
                return "Objects (" + group.numObjects() + ")";
            }
        }

        record GroupObjectSet(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group,
            @NotNull ClassTypeInfo info,
            @NotNull int[] indices
        ) implements Element {
            @Override
            public String toString() {
                return "%s (%d)".formatted(info.name(), indices.length);
            }
        }

        record Compound(
            @NotNull StreamingGraphResource graph,
            @NotNull StreamingGroupData group,
            int index
        ) implements Element {
            @Override
            public String toString() {
                ClassTypeInfo type = graph.types().get(group.typeStart() + index);
                return "[%d] %s".formatted(index, type.name());
            }
        }
    }

    @NotNull
    @Override
    public Element getRoot() {
        return new Element.Root(graph);
    }

    @NotNull
    @Override
    public List<? extends Element> getChildren(@NotNull Element parent) {
        return switch (parent) {
            case Element.Root(var graph) -> graph.groups().stream()
                .map(group -> new Element.Group(graph, group))
                .sorted()
                .toList();
            case Element.Group(var graph, var group) -> List.of(
                new Element.GroupObjects(graph, group),
                new Element.GroupRoots(graph, group),
                new Element.GroupDependencyGroups(graph, group),
                new Element.GroupDependentGroups(graph, group)
            );
            case Element.GroupRoots(var graph, var group) ->
                IntStream.range(group.rootStart(), group.rootStart() + group.rootCount())
                    .mapToObj(index -> {
                        var rootGroup = graph.group(graph.rootUUIDs().get(index));
                        var rootIndex = graph.rootIndices()[index];
                        Objects.requireNonNull(rootGroup);
                        return new Element.Compound(graph, rootGroup, rootIndex);
                    })
                    .toList();
            case Element.GroupDependencyGroups(var graph, var group) -> Arrays.stream(graph.subGroups(),
                    group.subGroupStart(),
                    group.subGroupStart() + group.subGroupCount())
                .mapToObj(graph::group)
                .map(Objects::requireNonNull)
                .map(subGroup -> new Element.Group(graph, subGroup))
                .toList();
            case Element.GroupDependentGroups(var graph, var group) -> graph.incomingGroups(group).stream()
                .sorted(Comparator.comparingInt(StreamingGroupData::groupID))
                .map(inGroup -> new Element.Group(graph, inGroup))
                .toList();
            case Element.GroupObjects(var graph, var group, var options) -> {
                if (options.contains(Element.GroupObjects.Options.GROUP_BY_TYPE)) {
                    var indices = IntStream.range(0, group.typeCount())
                        .boxed()
                        .collect(Collectors.groupingBy(index -> graph.types().get(group.typeStart() + index)));
                    yield indices.entrySet().stream()
                        .sorted(options.contains(Element.GroupObjects.Options.SORT_BY_COUNT)
                            ? Comparator.comparingInt((Map.Entry<ClassTypeInfo, List<Integer>> e) -> e.getValue().size()).reversed()
                            : Comparator.comparing((Map.Entry<ClassTypeInfo, List<Integer>> e) -> e.getKey().name().name()))
                        .map(entry -> new Element.GroupObjectSet(graph, group, entry.getKey(), entry.getValue().stream().mapToInt(x -> x).toArray()))
                        .toList();
                } else {
                    yield IntStream.range(0, group.typeCount())
                        .mapToObj(index -> new Element.Compound(graph, group, index))
                        .toList();
                }
            }
            case Element.GroupObjectSet(var graph, var group, var ignored, var indices) -> IntStream.of(indices)
                .mapToObj(index -> new Element.Compound(graph, group, index))
                .toList();
            case Element.Compound ignored -> List.of();
        };
    }

    @Override
    public boolean hasChildren(@NotNull Element node) {
        return switch (node) {
            case Element.Root(var graph) -> !graph.groups().isEmpty();
            case Element.Group ignored -> true;
            case Element.GroupObjects(var ignored, var group, var ignored1) -> group.numObjects() > 0;
            case Element.GroupObjectSet(var ignored, var ignored1, var ignored2, var indices) -> indices.length > 0;
            case Element.GroupDependencyGroups(var ignored, var group) -> group.subGroupCount() > 0;
            case Element.GroupDependentGroups(var ignored, var group) -> !graph.incomingGroups(group).isEmpty();
            case Element.GroupRoots(var ignored, var group) -> group.rootCount() > 0;
            case Element.Compound ignored -> false;
        };
    }

    @Override
    public String toString() {
        return "GraphStructure[]";
    }
}
