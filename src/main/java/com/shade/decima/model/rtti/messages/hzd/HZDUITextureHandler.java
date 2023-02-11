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

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "UITexture", game = GameType.HZD)
})
public class HZDUITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int smallTextureSize = buffer.getInt();
        final int bigTextureSize = buffer.getInt();

        if (smallTextureSize > 0) {
            final RTTIObject header = HZDTextureHeader.read(registry, buffer);
            final RTTIObject data = HZDTextureData.read(registry, buffer);
            final HwTexture texture = new HwTexture(header, data);
            object.set("SmallTexture", new RTTIObject(registry.find(HwTexture.class), texture));
        }

        if (bigTextureSize > 0) {
            final RTTIObject header = HZDTextureHeader.read(registry, buffer);
            final RTTIObject data = HZDTextureData.read(registry, buffer);
            final HwTexture texture = new HwTexture(header, data);
            object.set("BigTexture", new RTTIObject(registry.find(HwTexture.class), texture));
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject smallTexture = object.get("SmallTexture");
        final RTTIObject bigTexture = object.get("BigTexture");

        buffer.putInt(smallTexture != null ? smallTexture.<HwTexture>cast().getSize() : 0);
        buffer.putInt(bigTexture != null ? bigTexture.<HwTexture>cast().getSize() : 0);

        if (smallTexture != null) {
            smallTexture.<HwTexture>cast().write(registry, buffer);
        }

        if (bigTexture != null) {
            bigTexture.<HwTexture>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
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
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("SmallTexture", registry.find(HwTexture.class)),
            new Component("BigTexture", registry.find(HwTexture.class))
        };
    }
}
