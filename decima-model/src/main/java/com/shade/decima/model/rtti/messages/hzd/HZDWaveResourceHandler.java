package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hzd.HZDDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WaveResource", game = GameType.HZD)
})
public class HZDWaveResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        if (object.bool("IsStreaming")) {
            object.set("DataSource", HZDDataSource.read(registry, buffer));
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        if (object.bool("IsStreaming")) {
            object.obj("DataSource").<HwDataSource>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return object.bool("IsStreaming") ? object.obj("DataSource").<HwDataSource>cast().getSize() : 0;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("DataSource", registry.find(HwDataSource.class))
        };
    }
}
