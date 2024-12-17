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
    private static final boolean DEBUG = true;

    private final ObjectStreamingSystem system;
    private final StreamingGraphResource graph;
    private final TypeFactory factory;

    private GroupInfo currentGroup;
    private List<GroupInfo> currentSubGroups;

    private int streamingLinkIndex;
    private int streamingLocatorIndex;
    private int depth;

    public record GroupInfo(@NotNull StreamingGroupData group, @NotNull List<ObjectInfo> objects) {
        @Override
        public String toString() {
            return "GroupInfo[group=" + group + ", objects=" + objects.size() + "]";
        }
    }

    public record GroupResult(@NotNull List<GroupInfo> groups) {
        @NotNull
        public GroupInfo root() {
            return groups.getLast();
        }

        @Override
        public String toString() {
            return "Result[groups=" + groups.size() + "]";
        }
    }

    public record ObjectResult(@NotNull GroupResult group, @NotNull ObjectInfo object) {}

    public StreamingObjectReader(@NotNull ObjectStreamingSystem system, @NotNull TypeFactory factory) {
        this.system = system;
        this.graph = system.graph();
        this.factory = factory;
    }

    @NotNull
    public ObjectResult readObject(@NotNull String rootUUID) throws IOException {
        return readObject(parseUUID(rootUUID));
    }

    @NotNull
    public ObjectResult readObject(@NotNull GGUUID rootUUID) throws IOException {
        var group = Objects.requireNonNull(graph.group(rootUUID), () -> "Group not found: " + rootUUID);
        var index = Objects.requireNonNull(graph.rootIndex(rootUUID), () -> "Group not found: " + rootUUID);

        var groups = readGroup(group.groupID());
        var object = groups.root().objects().get(index);

        return new ObjectResult(groups, object);
    }

    @NotNull
    public GroupResult readGroup(int id) throws IOException {
        var groups = new ArrayList<GroupInfo>();
        readGroup(id, groups);

        return new GroupResult(groups);
    }

    @NotNull
    public GroupInfo readGroup(int id, @NotNull List<GroupInfo> groups) throws IOException {
        var group = Objects.requireNonNull(graph.group(id), () -> "Group not found: " + id);

        if (DEBUG) {
            log.debug("{}Reading group \033[34m{}\033[0m", "  ".repeat(depth), id);
        }

        for (GroupInfo result : groups) {
            if (result.group == group) {
                return result;
            }
        }

        depth++;

        var subGroups = new ArrayList<GroupInfo>(group.subGroupCount());
        for (int i = 0; i < group.subGroupCount(); i++) {
            subGroups.add(readGroup(graph.subGroups()[group.subGroupStart() + i], groups));
        }

        var objects = new ArrayList<ObjectInfo>(group.numObjects());
        for (int i = 0; i < group.numObjects(); i++) {
            var type = graph.types().get(group.typeStart() + objects.size());
            var object = (RTTIRefObject) type.newInstance();
            objects.add(new ObjectInfo(object, type));
        }

        var result = new GroupInfo(group, objects);

        currentSubGroups = subGroups;
        currentGroup = result;
        streamingLinkIndex = group.linkStart();
        streamingLocatorIndex = group.locatorStart();

        for (int i = 0, j = 0; i < group.spanCount(); i++) {
            var span = graph.spanTable().get(group.spanStart() + i);
            var data = getSpanData(span);
            var reader = BinaryReader.wrap(data);

            while (reader.remaining() > 0) {
                var object = objects.get(j++);

                if (DEBUG) {
                    log.debug("{}Reading \033[33m{}\033[0m at offset \033[34m{}\033[0m in \033[33m{}\033[0m", "  ".repeat(depth), object.info(), span.offset() + reader.position(), getSpanFile(span));
                }

                fillCompound(object, reader);
            }
        }

        depth--;
        groups.add(result);
        return result;
    }

    private void fillCompound(@NotNull ObjectInfo info, @NotNull BinaryReader reader) throws IOException {
        var object = info.object();
        for (ClassAttrInfo attr : info.info().serializableAttrs()) {
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
        Ref<?> ref;
        if (!reader.readByteBoolean()) {
            ref = null;
        } else if (info.name().name().equals("UUIDRef")) {
            ref = new UUIDRef<>((GGUUID) readCompound(factory.get(GGUUID.class), reader, factory));
        } else {
            ref = new StreamingLink<>();
        }
        if (ref != null) {
            resolveLink(info, ref);
        }
        return ref;
    }

    private void resolveStreamingDataSource(@NotNull StreamingDataSource dataSource) {
        if (dataSource.channel() != -1 && dataSource.length() > 0) {
            dataSource.locator(graph.locatorTable().get(streamingLocatorIndex++).data());
        }
    }

    private void resolveLink(@NotNull PointerTypeInfo info, @NotNull Ref<?> ref) {
        if (!(ref instanceof StreamingLink<?> streamingLink)) {
            return;
        }

        var result = system.readLink(streamingLinkIndex);
        int linkGroup = result.group();
        int linkIndex = result.index();

        streamingLinkIndex = result.position();

        if (info.name().name().equals("StreamingRef")) {
            // Can't resolve streaming references without actually running the game
            return;
        }

        GroupInfo group;
        if (linkGroup == -1) {
            // References the current group being read
            group = currentGroup;
        } else {
            // Seems to reference subgroups
            group = currentSubGroups.get(linkGroup);
        }

        var object = group.objects().get(linkIndex);
        var matches = info.itemType().get().type().isInstance(object.object());

        if (DEBUG) {
            log.debug(
                "{}Resolving \033[33m{}\033[0m to an object at index " +
                    "\033[34m{}\033[0m, group index: \033[34m{}\033[0m, in group " +
                    "\033[34m{}\033[0m (object: \033[33m{}\033[0m, matches: {})",
                "  ".repeat(depth),
                info.itemType().get(),
                linkIndex,
                linkGroup,
                group.group.groupID(),
                object.info(),
                matches ? "\033[32mtrue\033[0m" : "\033[31mfalse\033[0m"
            );
        }

        if (!matches) {
            throw new IllegalStateException("Type mismatch for pointer");
        }

        streamingLink.object = object.object();
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

    private static final class StreamingLink<T> implements Ref<T> {
        private Object object;

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) Objects.requireNonNull(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (StreamingLink<?>) obj;
            return Objects.equals(obj, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public String toString() {
            return "<pointer to object " + object + ">";
        }
    }

    private record UUIDRef<T>(@NotNull GGUUID objectUUID) implements Ref<T> {
        @Override
        public T get() {
            throw new NotImplementedException();
        }
    }
}
