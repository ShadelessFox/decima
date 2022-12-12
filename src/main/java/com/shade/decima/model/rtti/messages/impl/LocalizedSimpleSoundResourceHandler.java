package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(type = "LocalizedSimpleSoundResource", message = "MsgReadBinary", game = GameType.DS)
public class LocalizedSimpleSoundResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int bits = buffer.getShort() & 0xffff;
        // FIXME: This should be an instance of WaveResource
        final byte[] wave = IOUtils.getBytesExact(buffer, 21);
        final List<RTTIObject> entries = new ArrayList<>();

        // FIXME: There can't fit all 26 languages since the mask is uint16 which is just 16 bits long
        for (int i = 0; i < 26; i++) {
            if (((bits >>> i) & 1) == 0) {
                continue;
            }

            final ELanguage_Entry pair = new ELanguage_Entry();
            // FIXME: The language doesn't really match the contents
            pair.key = ((RTTITypeEnum) registry.find("ELanguage")).valueOf(i + 1);
            pair.value = Entry.read(registry, buffer);

            entries.add(new RTTIObject(registry.find(ELanguage_Entry.class), pair));
        }

        object.set("Entries", entries.toArray());
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
            new Component("Entries", registry.find(ELanguage_Entry[].class))
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(type = HwDataSource.class))
        public Object dataSource;
        @RTTIField(type = @Type(name = "uint8"))
        public byte unk;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new Entry();
            object.unk = buffer.get();
            object.dataSource = HwDataSource.read(registry, buffer);

            return new RTTIObject(registry.find(Entry.class), object);
        }
    }

    public static class ELanguage_Entry {
        @RTTIField(type = @Type(name = "ELanguage"))
        public Object key;
        @RTTIField(type = @Type(type = Entry.class))
        public Object value;
    }
}
