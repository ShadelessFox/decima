package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
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

public class HZDDataSource implements HwDataSource {
    @RTTIField(type = @Type(name = "String"))
    public String location;
    @RTTIField(type = @Type(name = "uint64"))
    public long offset;
    @RTTIField(type = @Type(name = "uint64"))
    public long length;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HZDDataSource();
        object.location = IOUtils.getString(buffer, buffer.getInt());
        object.offset = buffer.getLong();
        object.length = buffer.getLong();

        return new RTTIObject(registry.find(HZDDataSource.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] location = this.location.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(location.length);
        buffer.put(location);
        buffer.putLong(offset);
        buffer.putLong(length);
    }

    @Override
    public int getSize() {
        return location.getBytes(StandardCharsets.UTF_8).length + 20;
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager) throws IOException {
        if (!location.startsWith("cache:")) {
            throw new IOException("Data source points to a resource outside cache: " + location);
        }
        // TODO: Should prefix removal be handled by the manager itself?
        final String path = location.substring(6);
        final Packfile packfile = manager.findFirst(path);
        if (packfile == null) {
            throw new IOException("Can't find packfile that contains " + path);
        }
        return packfile.extract(path);
    }

    @Override
    public int getOffset() {
        return (int) offset;
    }

    @Override
    public int getLength() {
        return (int) length;
    }
}
