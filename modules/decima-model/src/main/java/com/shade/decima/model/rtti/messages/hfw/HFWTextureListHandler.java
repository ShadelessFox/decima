package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hfw.HFWTexture;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "TextureList", game = GameType.HFW),
})
public class HFWTextureListHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] entries = new RTTIObject[buffer.getInt()];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = TextureEntry.read(registry, buffer);
        }

        object.set("Entries", entries);
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
            new Component("Entries", registry.find(TextureEntry[].class))
        };
    }

    public static class TextureEntry {
        @RTTIField(type = @Type(name = "uint32"))
        public int streamingOffset;
        @RTTIField(type = @Type(name = "uint32"))
        public int streamingLength;
        @RTTIField(type = @Type(type = HwTexture.class))
        public RTTIObject texture;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var entry = new TextureEntry();
            entry.streamingOffset = buffer.getInt();
            entry.streamingLength = buffer.getInt();
            entry.texture = HFWTexture.read(registry, buffer);

            return new RTTIObject(registry.find(TextureEntry.class), entry);
        }
    }
}
