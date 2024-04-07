package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
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
    @Type(name = "MorphemeNetworkInstancePreInitializedData", game = GameType.HFW),
})
public class HFWMorphemeNetworkInstancePreInitializedDataHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var length = buffer.getInt();
        final var four = buffer.getInt();
        assert four == 4;
        if (length > 0) {
            object.set("Unk1", BufferUtils.getBytes(buffer, length));
            object.set("Unk2", BufferUtils.getInts(buffer, buffer.getInt()));
        }
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Unk1", factory.find("Array<uint8>")),
            new Component("Unk2", factory.find("Array<uint32>"))
        };
    }
}
