package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HwDataBuffer {
    @RTTIField(type = @Type(name = "uint32"))
    public int size;
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

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HwDataBuffer();
        object.size = buffer.getInt();
        object.mode = registry.<RTTITypeEnum>find("ERenderDataStreamingMode").valueOf(buffer.getInt());
        object.flags = buffer.getInt();
        object.format = registry.<RTTITypeEnum>find("EDataBufferFormat").valueOf(buffer.getInt());
        object.stride = buffer.getInt();

        if (!object.mode.toString().equals("Streaming")) {
            throw new IllegalStateException("Unsupported data buffer mode: " + object.mode);
        }

        object.dataSource = HwDataSource.read(registry, buffer);

        return new RTTIObject(registry.find(HwDataBuffer.class), object);
    }

    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        buffer.putInt(size);
        buffer.putInt(mode.value());
        buffer.putInt(flags);
        buffer.putInt(format.value());
        buffer.putInt(stride);
        dataSource.<HwDataSource>cast().write(registry, buffer);
    }

    public int getSize() {
        return 20 + dataSource.<HwDataBuffer>cast().getSize();
    }
}
