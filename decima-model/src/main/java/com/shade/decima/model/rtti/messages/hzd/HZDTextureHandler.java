package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hzd.HZDTextureData;
import com.shade.decima.model.rtti.types.hzd.HZDTextureHeader;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Texture", game = GameType.HZD),
})
public class HZDTextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Header", HZDTextureHeader.read(registry, buffer));
        object.set("Data", HZDTextureData.read(registry, buffer));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.obj("Header").<HwTextureHeader>cast().write(registry, buffer);
        object.obj("Data").<HwTextureData>cast().write(registry, buffer);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return object.obj("Header").<HwTextureHeader>cast().getSize() + object.obj("Data").<HwTextureData>cast().getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Header", registry.find(HwTextureHeader.class)),
            new Component("Data", registry.find(HwTextureData.class)),
        };
    }
}
