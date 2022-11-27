package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "DataBufferResource", message = "MsgReadBinary", game = GameType.DS)
public class DataBufferResourceHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass DataSource = RTTIUtils.newClassBuilder(registry, "DataSource")
            .member("Location", "String")
            .member("UUID", "GGUUID")
            .member("Channel", "uint32")
            .member("Offset", "uint32")
            .member("Length", "uint32")
            .build();

        final RTTITypeEnum ERenderDataStreamingMode = (RTTITypeEnum) registry.find("ERenderDataStreamingMode");
        final RTTITypeEnum EDataBufferFormat = (RTTITypeEnum) registry.find("EDataBufferFormat");

        final RTTITypeClass HwDataBuffer = RTTIUtils.newClassBuilder(registry, "HwDataBuffer")
            .member("Size", "uint32")
            .member("Mode", ERenderDataStreamingMode)
            .member("Flags", "uint32")
            .member("Format", EDataBufferFormat)
            .member("Stride", "uint32")
            .build();

        final RTTIObject data = HwDataBuffer.instantiate();
        data.set("Size", buffer.getInt());
        data.set("Mode", ERenderDataStreamingMode.valueOf(buffer.getInt()));
        data.set("Flags", buffer.getInt());
        data.set("Format", EDataBufferFormat.valueOf(buffer.getInt()));
        data.set("Stride", buffer.getInt());

        if (data.str("Mode").equals("Streaming")) {
            final RTTIObject dataSource = DataSource.instantiate();
            dataSource.set("Location", IOUtils.getString(buffer, buffer.getInt()));
            dataSource.set("UUID", registry.find("GGUUID").read(registry, buffer));
            dataSource.set("Channel", buffer.getInt());
            dataSource.set("Offset", buffer.getInt());
            dataSource.set("Length", buffer.getInt());

            data.define("DataSource", DataSource, dataSource);
        } else {
            throw new IllegalStateException("Unsupported data buffer mode: " + data.str("Mode"));
        }

        object.define("Data", HwDataBuffer, data);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
