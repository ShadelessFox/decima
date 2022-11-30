package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

@MessageHandlerRegistration(type = "TextureList", message = "MsgReadBinary", game = GameType.DS)
public class TextureListHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass TextureListEntry = RTTIUtils.newClassBuilder(registry, "Texture").build();
        final RTTITypeClass Texture = (RTTITypeClass) registry.find("Texture");

        final MessageHandler.ReadBinary textureHandler = Objects.requireNonNull(Texture.getMessageHandler("MsgReadBinary"));

        final int count = buffer.getInt();
        final RTTIObject[] textures = new RTTIObject[count];

        for (int i = 0; i < count; i++) {
            final RTTIObject entry = TextureListEntry.instantiate();
            textureHandler.read(registry, entry, buffer);

            textures[i] = entry;
        }

        object.define("Textures", new RTTITypeArray<>("Array", TextureListEntry), textures);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
