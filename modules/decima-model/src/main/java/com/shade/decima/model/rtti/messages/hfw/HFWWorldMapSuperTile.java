package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hzd.HZDTextureData;
import com.shade.decima.model.rtti.types.hzd.HZDTextureHeader;
import com.shade.decima.model.rtti.types.java.HwTexture;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "WorldMapSuperTile", game = GameType.HFW)
})
public class HFWWorldMapSuperTile implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int size0 = buffer.getInt();
        final int size1 = buffer.getInt();
        final int mask = buffer.getInt();

        if (size1 > 0) {
            BufferUtils.getBytes(buffer, size0);
            if ((mask & 0x1) != 0) {
                object.set("texture0", readTexture(buffer, registry));
            }
            if ((mask & 0x2) != 0) {
                object.set("texture1", readTexture(buffer, registry));
            }
            if ((mask & 0x4) != 0) {
                object.set("texture2", readTexture(buffer, registry));
            }
            if ((mask & 0x8) != 0) {
                object.set("texture3", readTexture(buffer, registry));
            }
        } else if (size0 > 0) {
            if ((mask & 0x1) != 0) {
                object.set("texture0", readTexture(buffer, registry));
            }
            if ((mask & 0x2) != 0) {
                object.set("texture1", readTexture(buffer, registry));
            }
            if ((mask & 0x4) != 0) {
                object.set("texture2", readTexture(buffer, registry));
            }
            if ((mask & 0x8) != 0) {
                object.set("texture3", readTexture(buffer, registry));
            }
            BufferUtils.getBytes(buffer, size1);
        }

    }

    @NotNull
    private static RTTIObject readTexture(@NotNull ByteBuffer buffer, RTTITypeRegistry registry) {
        final RTTIObject header = HZDTextureHeader.read(registry, buffer);
        final RTTIObject data = HZDTextureData.read(registry, buffer);
        return new RTTIObject(registry.find(HwTexture.class), new HwTexture(header, data));
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
            new Component("texture0", registry.find(HwTexture.class)),
            new Component("texture1", registry.find(HwTexture.class)),
            new Component("texture2", registry.find(HwTexture.class)),
            new Component("texture3", registry.find(HwTexture.class)),
        };
    }
}
