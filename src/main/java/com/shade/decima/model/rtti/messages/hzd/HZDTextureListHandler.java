package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hzd.HZDTextureData;
import com.shade.decima.model.rtti.types.hzd.HZDTextureHeader;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "TextureList", game = GameType.HZD),
})
public class HZDTextureListHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] textures = new RTTIObject[buffer.getInt()];

        for (int i = 0; i < textures.length; i++) {
            final RTTIObject header = HZDTextureHeader.read(registry, buffer);
            final RTTIObject data = HZDTextureData.read(registry, buffer);
            final HwTexture texture = new HwTexture(header, data);
            textures[i] = new RTTIObject(registry.find(HwTexture.class), texture);
        }

        object.set("Textures", textures);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] textures = object.objs("Textures");

        buffer.putInt(textures.length);

        for (RTTIObject texture : textures) {
            texture.<HwTexture>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return 4 + Arrays.stream(object.objs("Textures"))
            .map(RTTIObject::<HwTexture>cast)
            .mapToInt(HwTexture::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Textures", registry.find(HwTexture[].class))
        };
    }
}
