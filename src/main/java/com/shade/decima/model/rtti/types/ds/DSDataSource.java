package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;
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
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new DSDataSource();
        object.location = IOUtils.getString(buffer, buffer.getInt());
        object.uuid = registry.<RTTIClass>find("GGUUID").read(registry, buffer);
        object.channel = buffer.getInt();
        object.offset = buffer.getInt();
        object.length = buffer.getInt();

        return new RTTIObject(registry.find(DSDataSource.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] location = this.location.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(location.length);
        buffer.put(location);
        uuid.type().write(registry, buffer, uuid);
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
        final String path = "%s.core.stream".formatted(location);
        final Packfile packfile = manager.findFirst(path);
        if (packfile == null) {
            throw new IOException("Can't find packfile that contains " + path);
        }
        return packfile.extract(path);
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
