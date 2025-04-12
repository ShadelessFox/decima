package com.shade.decima.game.hfw.storage;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.rtti.data.ExtraBinaryDataHolder;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.runtime.ClassAttrInfo;
import com.shade.decima.rtti.runtime.ClassTypeInfo;
import com.shade.decima.rtti.runtime.PointerTypeInfo;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.*;

public class StreamingObjectReader extends HFWTypeReader {
    private static final Logger log = LoggerFactory.getLogger(StreamingObjectReader.class);

    private final ObjectStreamingSystem system;
    private final StreamingGraphResource graph;
    private final TypeFactory factory;

    private final LruWeakCache<StreamingGroupData, GroupResult> cache = new LruWeakCache<>(5000);

    private GroupResult currentGroup;
    private List<GroupResult> currentSubGroups;

    private boolean resolveStreamingLinksAndLocators;
    private int streamingLinkIndex;
    private int streamingLocatorIndex;
    private int depth;

    public record GroupResult(@NotNull StreamingGroupData group, @NotNull List<ObjectInfo> objects) {
        public GroupResult {
            objects = List.copyOf(objects);
        }

        @Override
        public String toString() {
            return "GroupInfo[group=" + group + ", objects=" + objects.size() + "]";
        }
    }

    public record ObjectResult(@NotNull GroupResult group, @NotNull ObjectInfo object) {
    }

    public StreamingObjectReader(@NotNull ObjectStreamingSystem system, @NotNull TypeFactory factory) {
        this.system = system;
        this.graph = system.graph();
        this.factory = factory;
    }

    @NotNull
    public GroupResult readGroup(int id) throws IOException {
        return readGroup(id, true);
    }

    @NotNull
    public GroupResult readGroup(int id, boolean readSubgroups) throws IOException {
        var groups = new ArrayList<GroupResult>();
        var result = readGroup(id, groups, readSubgroups);
        assert result == groups.getLast();
        return result;
    }

    @NotNull
    private GroupResult readGroup(int id, @NotNull List<GroupResult> groups, boolean readSubgroups) throws IOException {
        var group = Objects.requireNonNull(graph.group(id), () -> "Group not found: " + id);

        if (log.isDebugEnabled()) {
            log.debug("{}Reading group {}", indent(), Colors.blue(id));
        }

        for (GroupResult result : groups) {
            if (result.group == group) {
                return result;
            }
        }

        depth++;
        var result = readGroup(group, groups, readSubgroups);
        groups.add(result);
        depth--;

        return result;
    }

    @NotNull
    private GroupResult readGroup(@NotNull StreamingGroupData group, @NotNull List<GroupResult> groups, boolean readSubgroups) throws IOException {
        var subGroups = new ArrayList<GroupResult>(group.subGroupCount());
        if (readSubgroups) {
            for (int i = 0; i < group.subGroupCount(); i++) {
                subGroups.add(readGroup(graph.subGroups()[group.subGroupStart() + i], groups, true));
            }
        }

        currentSubGroups = subGroups;
        resolveStreamingLinksAndLocators = readSubgroups;

        GroupResult result = cache.get(group);
        if (result == null) {
            result = readSingleGroup(group);
            cache.put(group, result);
        }

        return result;
    }

    @NotNull
    private GroupResult readSingleGroup(@NotNull StreamingGroupData group) throws IOException {
        var objects = new ArrayList<ObjectInfo>(group.numObjects());
        for (int i = 0; i < group.numObjects(); i++) {
            var type = graph.types().get(group.typeStart() + objects.size());
            var object = (RTTIRefObject) type.newInstance();
            objects.add(new ObjectInfo(type, object));
        }

        var result = new GroupResult(group, objects);

        currentGroup = result;
        streamingLinkIndex = group.linkStart();
        streamingLocatorIndex = group.locatorStart();

        for (int i = 0, j = 0; i < group.spanCount(); i++) {
            var span = graph.spanTable().get(group.spanStart() + i);
            var data = getSpanData(span);
            var reader = BinaryReader.wrap(data);

            while (reader.remaining() > 0) {
                var object = objects.get(j++);

                if (log.isDebugEnabled()) {
                    log.debug(
                        "{}Reading {} in {} at offset {}",
                        indent(),
                        Colors.yellow(object.type()),
                        Colors.yellow(getSpanFile(span)),
                        Colors.blue(span.offset() + reader.position())
                    );
                }

                fillCompound(object, reader);
            }
        }

        return result;
    }

    private void fillCompound(@NotNull ObjectInfo info, @NotNull BinaryReader reader) throws IOException {
        var object = info.object();
        for (ClassAttrInfo attr : info.type().serializableAttrs()) {
            attr.set(object, read(attr.type().get(), reader, factory));
        }
        if (object instanceof ExtraBinaryDataHolder holder) {
            holder.deserialize(reader, factory);
        }
    }

    @NotNull
    @Override
    protected Object readCompound(@NotNull ClassTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Object object = super.readCompound(info, reader, factory);

        if (object instanceof StreamingDataSource dataSource) {
            resolveStreamingDataSource(dataSource);
        }

        return object;
    }

    @Nullable
    @Override
    protected Ref<?> readPointer(@NotNull PointerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        if (!reader.readByteBoolean()) {
            return null;
        } else if (info.name().name().equals("UUIDRef")) {
            return new UUIDRef<>((GGUUID) readCompound(factory.get(GGUUID.class), reader, factory));
        } else {
            return resolveStreamingLink(info);
        }
    }

    private void resolveStreamingDataSource(@NotNull StreamingDataSource dataSource) {
        if (!resolveStreamingLinksAndLocators) {
            return;
        }

        if (dataSource.channel() != -1 && dataSource.length() > 0) {
            var locator = graph.locatorTable().get(streamingLocatorIndex++).data();

            if (log.isDebugEnabled()) {
                log.debug(
                    "{}Resolving data source to {} at offset {}",
                    indent(),
                    Colors.yellow(graph.files().get(Math.toIntExact(locator & 0xffffff))),
                    Colors.blue(locator >>> 24)
                );
            }

            dataSource.locator(locator);
        }
    }

    @Nullable
    private Ref<?> resolveStreamingLink(@NotNull PointerTypeInfo info) {
        if (!resolveStreamingLinksAndLocators) {
            return null;
        }

        var result = system.readLink(streamingLinkIndex);
        int linkGroup = result.group();
        int linkIndex = result.index();

        streamingLinkIndex = result.position();

        if (info.name().name().equals("StreamingRef")) {
            // Can't resolve streaming references without actually running the game
            return null;
        }

        GroupResult group;
        if (linkGroup == -1) {
            // References the current group being read
            group = currentGroup;
        } else {
            // Seems to reference subgroups
            group = currentSubGroups.get(linkGroup);
        }

        var object = group.objects().get(linkIndex);
        var matches = info.itemType().get().type().isInstance(object.object());

        if (log.isDebugEnabled()) {
            log.debug(
                "{}Resolving {} to object {} (index: {}) in group {} (index: {})",
                indent(),
                Colors.yellow(info.name()),
                Colors.yellow(object.type()),
                Colors.blue(linkIndex),
                Colors.blue(group.group.groupID()),
                Colors.blue(linkGroup)
            );
        }

        if (!matches) {
            throw new IllegalStateException("Type mismatch for pointer");
        }

        return new StreamingLink<>(object.object());
    }

    @NotNull
    private String indent() {
        return "\t".repeat(depth);
    }

    @NotNull
    private byte[] getSpanData(@NotNull StreamingSourceSpan span) throws IOException {
        return system.getFileData(getSpanFile(span), span.offset(), span.length());
    }

    @NotNull
    private String getSpanFile(@NotNull StreamingSourceSpan span) {
        return graph.files().get(span.fileIndexAndIsPatch() & 0x7fffffff);
    }

    @NotNull
    private GGUUID parseUUID(@NotNull String objectUUID) {
        var uuid = UUID.fromString(objectUUID);
        var msb = uuid.getMostSignificantBits();
        var lsb = uuid.getLeastSignificantBits();

        var object = factory.newInstance(GGUUID.class);
        object.data0((byte) (msb >>> 56));
        object.data1((byte) (msb >>> 48));
        object.data2((byte) (msb >>> 40));
        object.data3((byte) (msb >>> 32));
        object.data4((byte) (msb >>> 24));
        object.data5((byte) (msb >>> 16));
        object.data6((byte) (msb >>> 8));
        object.data7((byte) (msb));
        object.data8((byte) (lsb >>> 56));
        object.data9((byte) (lsb >>> 48));
        object.data10((byte) (lsb >>> 40));
        object.data11((byte) (lsb >>> 32));
        object.data12((byte) (lsb >>> 24));
        object.data13((byte) (lsb >>> 16));
        object.data14((byte) (lsb >>> 8));
        object.data15((byte) (lsb));

        return object;
    }

    private record StreamingLink<T>(@NotNull RTTIRefObject object) implements Ref<T> {
        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) object;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "<streaming link to " + object.getType() + ">";
        }
    }

    private record UUIDRef<T>(@NotNull GGUUID objectUUID) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }

    private record Colors(@NotNull CharSequence text, int foreground) {
        Colors(@NotNull Object value, int foreground) {
            this(value.toString(), foreground);
        }

        @NotNull
        static Colors yellow(@NotNull Object value) {
            return new Colors(value, 33);
        }

        @NotNull
        static Colors blue(@NotNull Object value) {
            return new Colors(value, 34);
        }

        @Override
        public String toString() {
            return "\033[%dm%s\033[0m".formatted(foreground, text);
        }
    }
}
