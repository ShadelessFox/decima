package com.shade.decima.model.rtti.messages.dsdc;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "MorphemeAnimation", game = GameType.DSDC),
})
public class DSDCMorphemeAnimationHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Data", BufferUtils.getBytes(buffer, buffer.getInt()));
        object.set("Hash", factory.find("MurmurHashValue").read(factory, buffer));
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final byte[] data = object.get("Data");
        buffer.putInt(data.length);
        buffer.put(data);

        final RTTIObject hash = object.obj("Hash");
        hash.type().write(factory, buffer, hash);
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject object) {
        return 20 + object.<byte[]>get("Data").length;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Data", factory.find("Array<uint8>")),
            new Component("Hash", factory.find("MurmurHashValue"))
        };
    }
}
