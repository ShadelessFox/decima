package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.ds.DSDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WwiseWemResource", game = GameType.DS),
    @Type(name = "WwiseWemResource", game = GameType.DSDC)
})
public class DSWwiseWemResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        if (object.bool("IsStreaming")) {
            object.set("DataSource", DSDataSource.read(factory, reader, buffer));
        }
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        if (object.bool("IsStreaming")) {
            object.obj("DataSource").<HwDataSource>cast().write(factory, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        return object.bool("IsStreaming") ? object.obj("DataSource").<HwDataSource>cast().getSize() : 0;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("DataSource", factory.find(HwDataSource.class))
        };
    }
}
