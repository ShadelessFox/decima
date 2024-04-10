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
    @Type(name = "WwiseBankResource", game = GameType.DS),
    @Type(name = "WwiseBankResource", game = GameType.DSDC)
})
public class DSWwiseBankResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final int[] ids = object.get("WemIDs");
        final RTTIObject[] wems = new RTTIObject[ids.length];

        for (int i = 0; i < wems.length; i++) {
            wems[i] = DSDataSource.read(factory, reader, buffer);
        }

        object.set("DataSources", wems);
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        for (RTTIObject dataSource : object.objs("DataSources")) {
            dataSource.<HwDataSource>cast().write(factory, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        int size = 0;

        for (RTTIObject dataSource : object.objs("DataSources")) {
            size += dataSource.<HwDataSource>cast().getSize();
        }

        return size;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("DataSources", factory.find(HwDataSource[].class))
        };
    }
}
