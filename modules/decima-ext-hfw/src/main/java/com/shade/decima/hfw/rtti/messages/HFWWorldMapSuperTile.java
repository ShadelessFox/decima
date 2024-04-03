package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.hfw.rtti.types.HFWTexture;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WorldMapSuperTile", game = GameType.HFW)
})
public class HFWWorldMapSuperTile implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int size0 = buffer.getInt();
        final int size1 = buffer.getInt();
        final int mask = buffer.getInt();

        final List<RTTIObject> smallTextures = new ArrayList<>(4);
        final List<RTTIObject> bigTextures = new ArrayList<>(4);

        if (size0 > 0) {
            for (int i = 0; i < 4; i++) {
                if ((mask & (1 << i)) != 0) {
                    smallTextures.add(HFWTexture.read(registry, buffer));
                }
            }
        }

        if (size1 > 0) {
            for (int i = 0; i < 4; i++) {
                if ((mask & (1 << i)) != 0) {
                    bigTextures.add(HFWTexture.read(registry, buffer));
                }
            }
        }

        object.set("SmallTexture", smallTextures.toArray(RTTIObject[]::new));
        object.set("BigTexture", bigTextures.toArray(RTTIObject[]::new));
        object.set("Mask", mask);
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
            new Component("SmallTexture", registry.find(HwTexture[].class)),
            new Component("BigTexture", registry.find(HwTexture[].class)),
            new Component("Mask", registry.find("uint32"))
        };
    }
}
