package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.ds.DSDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WwiseBankResource", game = GameType.DS),
    @Type(name = "WwiseBankResource", game = GameType.DSDC)
})
public class DSWwiseBankResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int[] ids = object.get("WemIDs");
        final RTTIObject[] wems = new RTTIObject[ids.length];

        for (int i = 0; i < wems.length; i++) {
            wems[i] = DSDataSource.read(registry, buffer);
        }

        object.set("DataSources", wems);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (RTTIObject dataSource : object.objs("DataSources")) {
            dataSource.<HwDataSource>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        int size = 0;

        for (RTTIObject dataSource : object.objs("DataSources")) {
            size += dataSource.<HwDataSource>cast().getSize();
        }

        return size;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("DataSources", registry.find(HwDataSource[].class))
        };
    }
}
