package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.RTTIMessageHandler;
import com.shade.decima.model.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

@RTTIMessageHandler(type = "UITexture", message = "MsgReadBinary", game = GameType.DS)
public class UITextureHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final RTTITypeClass TextureEntry = RTTIUtils.newClassBuilder(registry, "Texture").build();
        final RTTITypeClass Texture = (RTTITypeClass) registry.find("Texture");

        final RTTIMessageReadBinary textureHandler = Objects.requireNonNull(Texture.getMessageHandler("MsgReadBinary"));

        final int textureSize0 = buffer.getInt();
        final int textureSize1 = buffer.getInt();

        if (textureSize0 > 0) {
            final RTTIObject entry = TextureEntry.instantiate();
            textureHandler.read(registry, entry, buffer);
            object.define("SmallTexture", TextureEntry, entry);
        }

        if (textureSize1 > 0) {
            final RTTIObject entry = TextureEntry.instantiate();
            textureHandler.read(registry, entry, buffer);
            object.define("BigTexture", TextureEntry, entry);
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
