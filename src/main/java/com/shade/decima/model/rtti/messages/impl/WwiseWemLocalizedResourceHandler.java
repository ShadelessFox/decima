package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.JavaObject;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(type = "WwiseWemLocalizedResource", message = "MsgReadBinary", game = GameType.DS)
public class WwiseWemLocalizedResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final int bits = buffer.getInt();
        final List<RTTIObject> entries = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            entries.add(Entry.read(registry, buffer));
        }

        object.set("Entries", entries.toArray());
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[] {
            new Component("Entries", registry.find(Entry[].class))
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(type = HwDataSource.class))
        public Object dataSource;
        @RTTIField(type = @Type(name = "uint64"))
        public long unk;

        @NotNull
        public static JavaObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new Entry();
            object.dataSource = HwDataSource.read(registry, buffer);
            object.unk = buffer.getLong();

            return new JavaObject(registry.find(Entry.class), object);
        }
    }
}
