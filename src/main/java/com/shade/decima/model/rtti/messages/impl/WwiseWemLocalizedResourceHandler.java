package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(type = "WwiseWemLocalizedResource", message = "MsgReadBinary", game = GameType.DS)
public class WwiseWemLocalizedResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass DataSource = RTTIUtils.newClassBuilder(registry, "DataSource")
            .member("Location", "String")
            .member("UUID", "GGUUID")
            .member("Channel", "uint32")
            .member("Offset", "uint32")
            .member("Length", "uint32")
            .build();

        final RTTITypeClass Entry = RTTIUtils.newClassBuilder(registry, "Entry")
            .member("DataSource", DataSource)
            .member("Unk", "uint64")
            .build();

        final int bits = buffer.getInt();
        final List<RTTIObject> entries = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            final RTTIObject entry = Entry.instantiate();

            final RTTIObject dataSource = DataSource.instantiate();
            dataSource.set("Location", IOUtils.getString(buffer, buffer.getInt()));
            dataSource.set("UUID", registry.find("GGUUID").read(registry, buffer));
            dataSource.set("Channel", buffer.getInt());
            dataSource.set("Offset", buffer.getInt());
            dataSource.set("Length", buffer.getInt());

            entry.set("DataSource", dataSource);
            entry.set("Unk", buffer.getLong());

            entries.add(entry);
        }

        object.define("Entries", new RTTITypeArray<>("Array", Entry), entries.toArray());
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
