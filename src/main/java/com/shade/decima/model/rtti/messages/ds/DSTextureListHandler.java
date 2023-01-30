package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.ds.DSTextureData;
import com.shade.decima.model.rtti.types.ds.DSTextureHeader;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "TextureList", game = GameType.DS),
    @Type(name = "TextureList", game = GameType.DSDC)
})
public class DSTextureListHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int count = buffer.getInt();
        final RTTIObject[] textures = new RTTIObject[count];

        for (int i = 0; i < count; i++) {
            textures[i] = Texture.read(registry, buffer);
        }

        object.set("Textures", textures);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] textures = object.objs("Textures");

        buffer.putInt(textures.length);

        for (RTTIObject texture : textures) {
            texture.<Texture>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return 4 + Arrays.stream(object.objs("Textures"))
            .map(RTTIObject::<Texture>cast)
            .mapToInt(Texture::getSize)
            .sum();
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
            object.header = DSTextureHeader.read(registry, buffer);
            object.data = DSTextureData.read(registry, buffer);

            return new RTTIObject(registry.find(Texture.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            header.<HwTextureHeader>cast().write(registry, buffer);
            header.<HwTextureData>cast().write(registry, buffer);
        }

        public int getSize() {
            return header.<HwTextureHeader>cast().getSize() + data.<HwTextureData>cast().getSize();
        }
    }
}
