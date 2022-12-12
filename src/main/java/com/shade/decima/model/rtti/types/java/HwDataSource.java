package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HwDataSource {
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
        final var object = new HwDataSource();
        object.location = IOUtils.getString(buffer, buffer.getInt());
        object.uuid = registry.<RTTIClass>find("GGUUID").read(registry, buffer);
        object.channel = buffer.getInt();
        object.offset = buffer.getInt();
        object.length = buffer.getInt();

        return new RTTIObject(registry.find(HwDataSource.class), object);
    }

    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] location = this.location.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(location.length);
        buffer.put(location);
        uuid.type().write(registry, buffer, uuid);
        buffer.putInt(channel);
        buffer.putInt(offset);
        buffer.putInt(length);
    }

    public int getSize() {
        final byte[] location = this.location.getBytes(StandardCharsets.UTF_8);
        return location.length + 32;
    }
}
