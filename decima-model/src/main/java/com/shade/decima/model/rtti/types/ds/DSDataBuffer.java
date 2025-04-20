package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class DSDataBuffer {
    @RTTIField(type = @Type(name = "uint32"))
    public int count;
    @RTTIField(type = @Type(name = "ERenderDataStreamingMode"))
    public RTTITypeEnum.Constant mode;
    @RTTIField(type = @Type(name = "uint32"))
    public int flags;
    @RTTIField(type = @Type(name = "EDataBufferFormat"))
    public RTTITypeEnum.Constant format;
    @RTTIField(type = @Type(name = "uint32"))
    public int stride;
    @RTTIField(type = @Type(type = HwDataSource.class))
    public RTTIObject dataSource;
    @RTTIField(type = @Type(name = "Array<uint8>"))
    public byte[] data;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new DSDataBuffer();
        object.count = buffer.getInt();
        object.mode = registry.<RTTITypeEnum>find("ERenderDataStreamingMode").valueOf(buffer.getInt());
        object.flags = buffer.getInt();
        object.format = registry.<RTTITypeEnum>find("EDataBufferFormat").valueOf(buffer.getInt());
        object.stride = buffer.getInt();

        switch (object.mode.toString()) {
            case "Streaming" -> object.dataSource = DSDataSource.read(registry, buffer);
            case "NotStreaming" -> object.data = BufferUtils.getBytes(buffer, object.count * object.stride);
            default -> throw new IllegalStateException("Unsupported buffer mode: " + object.mode);
        }

        return new RTTIObject(registry.find(DSDataBuffer.class), object);
    }

    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        buffer.putInt(count);
        buffer.putInt(mode.value());
        buffer.putInt(flags);
        buffer.putInt(format.value());
        buffer.putInt(stride);

        if (dataSource != null) {
            dataSource.<HwDataSource>cast().write(registry, buffer);
        } else {
            buffer.put(data);
        }
    }

    public int getSize() {
        return 20 + (dataSource != null ? dataSource.<HwDataSource>cast().getSize() : data.length);
    }
}
