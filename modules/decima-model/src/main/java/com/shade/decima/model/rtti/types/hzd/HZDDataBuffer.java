package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HZDDataBuffer {
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
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final var object = new HZDDataBuffer();
        object.count = buffer.getInt();
        object.mode = factory.<RTTITypeEnum>find("ERenderDataStreamingMode").valueOf(buffer.getInt());
        object.flags = buffer.getInt();
        object.format = factory.<RTTITypeEnum>find("EDataBufferFormat").valueOf(buffer.getInt());
        object.stride = buffer.getInt();

        switch (object.mode.toString()) {
            case "Streaming" -> object.dataSource = HZDDataSource.read(factory, buffer);
            case "NotStreaming" -> object.data = BufferUtils.getBytes(buffer, object.count * object.stride);
            default -> throw new IllegalStateException("Unsupported buffer mode: " + object.mode);
        }

        return new RTTIObject(factory.find(HZDDataBuffer.class), object);
    }

    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        buffer.putInt(count);
        buffer.putInt(mode.value());
        buffer.putInt(flags);
        buffer.putInt(format.value());
        buffer.putInt(stride);

        if (dataSource != null) {
            dataSource.<HwDataSource>cast().write(factory, buffer);
        } else {
            buffer.put(data);
        }
    }

    public int getSize() {
        return 20 + (dataSource != null ? dataSource.<HwDataSource>cast().getSize() : data.length);
    }
}
