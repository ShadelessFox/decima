package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "MorphemeAnimation", game = GameType.HFW),
})
public class HFWMorphemeAnimationHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        object.set("Unk1", BufferUtils.getBytes(buffer, buffer.getInt()));
        object.set("Unk2", factory.find("MurmurHashValue").read(factory, reader, buffer));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Unk1", factory.find("Array<uint8>")),
            new Component("Unk2", factory.find("MurmurHashValue"))
        };
    }
}
