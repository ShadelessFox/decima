package com.shade.decima.game.hfw.storage;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.factory.TypeFactory;
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

    private int streamingLinkIndex;
    private int streamingLocatorIndex;
    private int depth;

    public record GroupReadResult(@NotNull StreamingGroupData group, @NotNull List<ObjectInfo> objects) {}

    public StreamingObjectReader(@NotNull ObjectStreamingSystem system, @NotNull TypeFactory factory) {
        this.system = system;
        this.graph = system.graph();
        this.factory = factory;
    }

    @NotNull
    public ObjectInfo readObject(@NotNull String rootUUID) throws IOException {
        return readObject(parseUUID(rootUUID));
    }

    @NotNull
    public ObjectInfo readObject(@NotNull GGUUID rootUUID) throws IOException {
        var group = Objects.requireNonNull(graph.group(rootUUID), () -> "Group not found: " + rootUUID);
        var index = Objects.requireNonNull(graph.rootIndex(rootUUID), () -> "Group not found: " + rootUUID);
        var result = readGroup(group.groupID());

        throw new NotImplementedException();
    }

    @NotNull
    public Object readGroup(int id) throws IOException {
        var groups = new ArrayList<GroupReadResult>();
        readGroup(id, groups);

        throw new NotImplementedException();
    }

    @NotNull
    public GroupReadResult readGroup(int id, @NotNull List<GroupReadResult> groups) throws IOException {
        var group = Objects.requireNonNull(graph.group(id), () -> "Group not found: " + id);

        if (DEBUG) {
            log.info("{}Reading group \033[34m{}\033[0m", "  ".repeat(depth), id);
        }

        for (GroupReadResult result : groups) {
            if (result.group == group) {
                return result;
            }
        }

        depth++;

        var subGroups = new ArrayList<GroupReadResult>(group.subGroupCount());
        for (int i = 0; i < group.subGroupCount(); i++) {
            subGroups.add(readGroup(graph.subGroups()[group.subGroupStart() + i], groups));
        }

        var objects = new ArrayList<ObjectInfo>(group.numObjects());
        for (int i = 0; i < group.spanCount(); i++) {
            var span = graph.spanTable().get(group.spanStart() + i);
            var data = getSpanData(span);
            var reader = BinaryReader.wrap(data);

            while (reader.remaining() > 0) {
                var type = graph.types().get(group.typeStart() + objects.size());

                if (DEBUG) {
                    log.info("{}Reading \033[33m{}\033[0m at offset \033[34m{}\033[0m in \033[33m{}\033[0m", "  ".repeat(depth), type, span.offset() + reader.position(), getSpanFile(span));
                }

                var object = (RTTIRefObject) readCompound(type, reader, factory);
                objects.add(new ObjectInfo(object, type));
            }
        }

        var result = new GroupReadResult(group, objects);

        depth--;
        groups.add(result);

        return result;
    }

    @NotNull
    @Override
    protected Object readCompound(@NotNull ClassTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Object object = super.readCompound(info, reader, factory);

        if (object instanceof StreamingDataSource dataSource && dataSource.channel() != -1 && dataSource.length() > 0) {
            var locator = graph.locatorTable().get(streamingLocatorIndex++);
            log.info("Reading data source \033[33m{}\033[0m at \033[34m{}\033[0m", dataSource, locator);
            // TODO: Resolve
        }

        return object;
    }

    @Nullable
    @Override
    protected Ref<?> readPointer(@NotNull PointerTypeInfo info, @NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        // TODO: Resolve
        return super.readPointer(info, reader, factory);
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
}
