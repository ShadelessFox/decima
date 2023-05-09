package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
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
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int bits = buffer.getInt();
        final List<RTTIObject> entries = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            entries.add(Entry.read(registry, buffer));
        }

        object.set("Entries", entries.toArray(RTTIObject[]::new));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Entries", registry.find(Entry[].class))
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(type = HwDataSource.class))
        public Object dataSource;
        @RTTIField(type = @Type(name = "uint64"))
        public long unk;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new Entry();
            object.dataSource = DSDataSource.read(registry, buffer);
            object.unk = buffer.getLong();

            return new RTTIObject(registry.find(Entry.class), object);
        }
    }
}
