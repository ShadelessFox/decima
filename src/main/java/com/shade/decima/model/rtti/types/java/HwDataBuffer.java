package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HwDataBuffer {
    @RTTIField(type = @Type(name = "uint32"))
    public int size;
    @RTTIField(type = @Type(name = "ERenderDataStreamingMode"))
    public Object mode;
    @RTTIField(type = @Type(name = "uint32"))
    public int flags;
    @RTTIField(type = @Type(name = "EDataBufferFormat"))
    public Object format;
    @RTTIField(type = @Type(name = "uint32"))
    public int stride;
    @RTTIField(type = @Type(type = HwDataSource.class))
    public Object dataSource;

    @NotNull
    public static JavaObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HwDataBuffer();
        object.size = buffer.getInt();
        object.mode = ((RTTITypeEnum) registry.find("ERenderDataStreamingMode")).valueOf(buffer.getInt());
        object.flags = buffer.getInt();
        object.format = ((RTTITypeEnum) registry.find("EDataBufferFormat")).valueOf(buffer.getInt());
        object.stride = buffer.getInt();

        if (!object.mode.toString().equals("Streaming")) {
            throw new IllegalStateException("Unsupported data buffer mode: " + object.mode);
        }

        object.dataSource = HwDataSource.read(registry, buffer);

        return new JavaObject(registry.find(HwDataBuffer.class), object);
    }
}
