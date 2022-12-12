package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

@MessageHandlerRegistration(type = "UITexture", message = "MsgReadBinary", game = GameType.DS)
public class UITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTITypeClass Texture = registry.find("Texture");
        final RTTIClass.Message<ReadBinary> message = Objects.requireNonNull(Texture.getMessage("MsgReadBinary"));
        final MessageHandler.ReadBinary handler = Objects.requireNonNull(message.getHandler());

        final int textureSize0 = buffer.getInt();
        final int textureSize1 = buffer.getInt();

        if (textureSize0 > 0) {
            final RTTIObject entry = Texture.instantiate();
            handler.read(registry, buffer, entry);
            object.set("SmallTexture", entry);
        }

        if (textureSize1 > 0) {
            final RTTIObject entry = Texture.instantiate();
            handler.read(registry, buffer, entry);
            object.set("BigTexture", entry);
        }
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
            new Component("SmallTexture", registry.find("Texture")),
            new Component("BigTexture", registry.find("Texture"))
        };
    }

}
