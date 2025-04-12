package com.shade.decima.game.hfw;

import com.shade.decima.game.hfw.game.ForbiddenWestGame;
import com.shade.decima.game.hfw.storage.StreamingGraphResource;
import com.shade.decima.game.hfw.storage.StreamingObjectReader;
import com.shade.decima.rtti.runtime.TypedObject;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A utility class for traversing the graph of objects of a specific type.
 */
public final class GraphWalker {
    private static final Logger log = LoggerFactory.getLogger(GraphWalker.class);

    private GraphWalker() {
    }

    @NotNull
    public static <T extends TypedObject> Iterable<SearchResult<T>> iterate(
        @NotNull Class<T> type,
        @NotNull ForbiddenWestGame game,
        boolean readSubgroups
    ) throws IOException {
        return stream(type, game.getStreamingReader(), game.getStreamingGraph(), readSubgroups)::iterator;
    }

    @NotNull
    public static <T extends TypedObject> Stream<SearchResult<T>> stream(
        @NotNull Class<T> type,
        @NotNull StreamingObjectReader reader,
        @NotNull StreamingGraphResource graph,
        boolean readSubgroups
    ) throws IOException {
        return StreamSupport.stream(spliterator(type, reader, graph, readSubgroups), false);
    }

    @NotNull
    private static <T extends TypedObject> Spliterator<SearchResult<T>> spliterator(
        @NotNull Class<T> ofType,
        @NotNull StreamingObjectReader reader,
        @NotNull StreamingGraphResource graph,
        boolean readSubgroups
    ) throws IOException {
        return new Spliterator<>() {
            private StreamingObjectReader.GroupResult result;
            private int nextGroupIndex;
            private int nextObjectIndex;

            @Override
            public boolean tryAdvance(Consumer<? super SearchResult<T>> action) {
                do {
                    if (tryAdvanceObject(action)) {
                        return true;
                    }
                } while (tryAdvanceGroup());
                return false;
            }

            @Override
            public Spliterator<SearchResult<T>> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return ORDERED | NONNULL;
            }

            private boolean tryAdvanceGroup() {
                var groups = graph.groups();
                for (int i = nextGroupIndex; i < groups.size(); i++) {
                    var group = groups.get(i);
                    if (graph.types(group).stream().anyMatch(t -> t.isInstanceOf(ofType))) {
                        log.debug("Reading group {}/{}", i + 1, groups.size());
                        try {
                            result = reader.readGroup(group.groupID(), readSubgroups);
                            nextGroupIndex = i + 1;
                            nextObjectIndex = 0;
                            return true;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
                return false;
            }

            private boolean tryAdvanceObject(Consumer<? super SearchResult<T>> action) {
                if (result != null) {
                    var objects = result.objects();
                    for (int i = nextObjectIndex; i < objects.size(); i++) {
                        var object = objects.get(i).object();
                        if (ofType.isInstance(object)) {
                            action.accept(new SearchResult<>(result.group().groupID(), i, ofType.cast(object)));
                            nextObjectIndex = i + 1;
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public record SearchResult<T>(int groupId, int objectIndex, T object) {
    }
}
