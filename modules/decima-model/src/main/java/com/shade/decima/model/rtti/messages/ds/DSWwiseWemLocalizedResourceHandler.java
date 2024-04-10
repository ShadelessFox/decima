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
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WwiseWemLocalizedResource", game = GameType.DS),
    @Type(name = "WwiseWemLocalizedResource", game = GameType.DSDC)
})
public class DSWwiseWemLocalizedResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final int bits = buffer.getInt();
        final List<RTTIObject> entries = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            entries.add(Entry.read(factory, reader, buffer));
        }

        object.set("Entries", entries.toArray(RTTIObject[]::new));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Entries", factory.find(Entry[].class))
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(type = HwDataSource.class))
        public Object dataSource;
        @RTTIField(type = @Type(name = "uint64"))
        public long unk;

        @NotNull
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
            final var object = new Entry();
            object.dataSource = DSDataSource.read(factory, reader, buffer);
            object.unk = buffer.getLong();

            return new RTTIObject(factory.find(Entry.class), object);
        }
    }
}
