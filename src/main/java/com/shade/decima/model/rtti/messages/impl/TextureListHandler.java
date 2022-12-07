package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.impl.TextureHandler.HwTextureData;
import com.shade.decima.model.rtti.messages.impl.TextureHandler.HwTextureHeader;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(type = "TextureList", message = "MsgReadBinary", game = GameType.DS)
public class TextureListHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        final int count = buffer.getInt();
        final RTTIObject[] textures = new RTTIObject[count];

        for (int i = 0; i < count; i++) {
            textures[i] = Texture.read(registry, buffer);
        }

        object.set("Textures", textures);
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Textures", registry.find(Texture[].class))
        };
    }

    // TODO: This type represents the Texture class without the RTTIRefObject's fields.
    //       It's not ideal to "extend" it just to make the viewer work. We need to find
    //       a better solution
    @RTTIExtends(@Type(name = "Texture"))
    public static class Texture {
        @RTTIField(type = @Type(type = HwTextureHeader.class))
        public RTTIObject header;
        @RTTIField(type = @Type(type = HwTextureData.class))
        public RTTIObject data;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var object = new Texture();
            object.header = HwTextureHeader.read(registry, buffer);
            object.data = HwTextureData.read(registry, buffer);

            return new RTTIObject(registry.find(Texture.class), object);
        }
    }
}
