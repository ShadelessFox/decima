package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(type = "AnimationStreamingEventResource", message = "MsgReadBinary", game = GameType.DS)
public class AnimationStreamingEventResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTIObject dataSource = RTTIUtils.newClassBuilder(registry, "DataSource")
            .member("Location", "String")
            .member("UUID", "GGUUID")
            .member("Channel", "uint32")
            .member("Offset", "uint32")
            .member("Length", "uint32")
            .build().instantiate();

        dataSource.set("Location", IOUtils.getString(buffer, buffer.getInt()));
        dataSource.set("UUID", registry.find("GGUUID").read(registry, buffer));
        dataSource.set("Channel", buffer.getInt());
        dataSource.set("Offset", buffer.getInt());
        dataSource.set("Length", buffer.getInt());

        object.define("DataSource", dataSource.type(), dataSource);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
