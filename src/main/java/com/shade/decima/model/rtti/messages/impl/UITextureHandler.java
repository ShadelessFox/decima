package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

@MessageHandlerRegistration(type = "UITexture", message = "MsgReadBinary", game = GameType.DS)
public class UITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass TextureEntry = RTTIUtils.newClassBuilder(registry, "Texture").build();
        final RTTITypeClass Texture = (RTTITypeClass) registry.find("Texture");

        final RTTIClass.Message<ReadBinary> message = Objects.requireNonNull(Texture.getMessage("MsgReadBinary"));
        final MessageHandler.ReadBinary handler = Objects.requireNonNull(message.getHandler());

        final int textureSize0 = buffer.getInt();
        final int textureSize1 = buffer.getInt();

        if (textureSize0 > 0) {
            final RTTIObject entry = TextureEntry.instantiate();
            handler.read(registry, entry, buffer);
            object.define("SmallTexture", TextureEntry, entry);
        }

        if (textureSize1 > 0) {
            final RTTIObject entry = TextureEntry.instantiate();
            handler.read(registry, entry, buffer);
            object.define("BigTexture", TextureEntry, entry);
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
