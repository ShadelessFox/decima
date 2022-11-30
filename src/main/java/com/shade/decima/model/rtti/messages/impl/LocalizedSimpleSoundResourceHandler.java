package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeHashMap;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(type = "LocalizedSimpleSoundResource", message = "MsgReadBinary", game = GameType.DS)
public class LocalizedSimpleSoundResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeEnum ELanguage = (RTTITypeEnum) registry.find("ELanguage");

        final RTTITypeClass DataSource = RTTIUtils.newClassBuilder(registry, "DataSource")
            .member("Location", "String")
            .member("UUID", "GGUUID")
            .member("Channel", "uint32")
            .member("Offset", "uint32")
            .member("Length", "uint32")
            .build();

        final RTTITypeClass Entry = RTTIUtils.newClassBuilder(registry, "Entry")
            .member("DataSource", DataSource)
            .member("Unk", "uint8")
            .build();

        final RTTITypeClass ELanguage_Entry = RTTIUtils.newClassBuilder(registry, "ELanguage_Entry")
            .member("Key", ELanguage)
            .member("Value", Entry)
            .build();

        final RTTITypeHashMap HashMap_ELanguage_DataSource = new RTTITypeHashMap("HashMap", ELanguage_Entry);

        final int bits = buffer.getShort() & 0xffff;
        // FIXME: This should be an instance of WaveResource
        final byte[] wave = IOUtils.getBytesExact(buffer, 21);
        final List<RTTIObject> entries = new ArrayList<>();

        // FIXME: There can't fit all 26 languages since the mask is uint16 which is just 16 bits long
        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            final RTTIObject entry = Entry.instantiate();
            entry.set("Unk", buffer.get());

            final RTTIObject dataSource = DataSource.instantiate();
            dataSource.set("Location", IOUtils.getString(buffer, buffer.getInt()));
            dataSource.set("UUID", registry.find("GGUUID").read(registry, buffer));
            dataSource.set("Channel", buffer.getInt());
            dataSource.set("Offset", buffer.getInt());
            dataSource.set("Length", buffer.getInt());

            entry.set("DataSource", dataSource);

            final RTTIObject pair = ELanguage_Entry.instantiate();
            // FIXME: The language doesn't really match the contents
            pair.set("Key", ELanguage.valueOf(i + 1));
            pair.set("Value", entry);

            entries.add(pair);
        }

        object.define("Entries", HashMap_ELanguage_DataSource, entries.toArray());
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
