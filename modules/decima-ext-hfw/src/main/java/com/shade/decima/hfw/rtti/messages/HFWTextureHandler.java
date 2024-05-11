package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.hfw.rtti.types.HFWTextureData;
import com.shade.decima.hfw.rtti.types.HFWTextureHeader;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Texture", game = GameType.HFW),
})
public class HFWTextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        object.set("Header", HFWTextureHeader.read(factory, buffer));
        object.set("Data", HFWTextureData.read(factory, buffer));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        object.obj("Header").<HwTextureHeader>cast().write(factory, buffer);
        object.obj("Data").<HwTextureData>cast().write(factory, buffer);
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        return object.obj("Header").<HwTextureHeader>cast().getSize() + object.obj("Data").<HwTextureData>cast().getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Header", factory.find(HwTextureHeader.class)),
            new Component("Data", factory.find(HwTextureData.class)),
        };
    }
}