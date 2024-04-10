package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DSDataSource implements HwDataSource {
    @RTTIField(type = @Type(name = "String"))
    public String location;
    @RTTIField(type = @Type(name = "GGUUID"), name = "UUID")
    public RTTIObject uuid;
    @RTTIField(type = @Type(name = "uint32"))
    public int channel;
    @RTTIField(type = @Type(name = "uint32"))
    public int offset;
    @RTTIField(type = @Type(name = "uint32"))
    public int length;

    @NotNull
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var object = new DSDataSource();
        object.location = BufferUtils.getString(buffer, buffer.getInt());
        object.uuid = factory.<RTTIClass>find("GGUUID").read(factory, reader, buffer);
        object.channel = buffer.getInt();
        object.offset = buffer.getInt();
        object.length = buffer.getInt();

        return new RTTIObject(factory.find(DSDataSource.class), object);
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final byte[] location = this.location.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(location.length);
        buffer.put(location);
        uuid.type().write(factory, buffer, uuid);
        buffer.putInt(channel);
        buffer.putInt(offset);
        buffer.putInt(length);
    }

    @Override
    public int getSize() {
        return location.getBytes(StandardCharsets.UTF_8).length + 32;
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager) throws IOException {
        return getData(manager, getOffset(), getLength());
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int offset, int length) throws IOException {
        final ArchiveFile file = manager.getFile("%s.core.stream".formatted(location));
        try (InputStream is = file.newInputStream()) {
            if (offset > 0) {
                is.skipNBytes(offset);
            }

            if (length > 0) {
                return is.readNBytes(length);
            } else {
                return is.readAllBytes();
            }
        }
    }

    @NotNull
    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }
}
