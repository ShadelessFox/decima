package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StreamHandle {
    @RTTIField(type = @Type(type = String.class))
    public String resourcePath;
    @RTTIField(type = @Type(name = "uint64"))
    public long resourceOffset;
    @RTTIField(type = @Type(name = "uint64"))
    public long resourceLength;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new StreamHandle();
        int stringSize = buffer.getInt();
        object.resourcePath = IOUtils.getString(buffer, stringSize);
        object.resourceOffset = buffer.getLong();
        object.resourceLength = buffer.getLong();
        return new RTTIObject(registry.find(StreamHandle.class), object);
    }

    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] location = resourcePath.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(location.length);
        buffer.put(location);
        buffer.putLong(resourceOffset);
        buffer.putLong(resourceLength);
    }

    public int getSize() {
        final byte[] location = resourcePath.getBytes(StandardCharsets.UTF_8);
        return 4 + location.length + 16;
    }
}
