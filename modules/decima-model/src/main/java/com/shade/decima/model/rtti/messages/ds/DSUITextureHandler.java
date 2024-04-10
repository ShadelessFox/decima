package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.ds.DSTextureData;
import com.shade.decima.model.rtti.types.ds.DSTextureHeader;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "UITexture", game = GameType.DS),
    @Type(name = "UITexture", game = GameType.DSDC)
})
public class DSUITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final int smallTextureSize = buffer.getInt();
        final int bigTextureSize = buffer.getInt();

        if (smallTextureSize > 0) {
            final RTTIObject header = DSTextureHeader.read(factory, buffer);
            final RTTIObject data = DSTextureData.read(factory, reader, buffer);
            final HwTexture texture = new HwTexture(header, data);
            object.set("SmallTexture", new RTTIObject(factory.find(HwTexture.class), texture));
        }

        if (bigTextureSize > 0) {
            final RTTIObject header = DSTextureHeader.read(factory, buffer);
            final RTTIObject data = DSTextureData.read(factory, reader, buffer);
            final HwTexture texture = new HwTexture(header, data);
            object.set("BigTexture", new RTTIObject(factory.find(HwTexture.class), texture));
        }
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final RTTIObject smallTexture = object.get("SmallTexture");
        final RTTIObject bigTexture = object.get("BigTexture");

        buffer.putInt(smallTexture != null ? smallTexture.<HwTexture>cast().getSize() : 0);
        buffer.putInt(bigTexture != null ? bigTexture.<HwTexture>cast().getSize() : 0);

        if (smallTexture != null) {
            smallTexture.<HwTexture>cast().write(factory, buffer);
        }

        if (bigTexture != null) {
            bigTexture.<HwTexture>cast().write(factory, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        final RTTIObject smallTexture = object.get("SmallTexture");
        final RTTIObject bigTexture = object.get("BigTexture");

        int size = 8;

        if (smallTexture != null) {
            size += smallTexture.<HwTexture>cast().getSize();
        }

        if (bigTexture != null) {
            size += bigTexture.<HwTexture>cast().getSize();
        }

        return size;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("SmallTexture", factory.find(HwTexture.class)),
            new Component("BigTexture", factory.find(HwTexture.class))
        };
    }
}
