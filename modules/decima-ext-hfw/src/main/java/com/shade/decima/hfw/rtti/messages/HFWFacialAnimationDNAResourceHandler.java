package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.hfw.data.riglogic.RigLogic;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "FacialAnimationDNAResource", game = GameType.HFW),
})
public class HFWFacialAnimationDNAResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        buffer.order(ByteOrder.BIG_ENDIAN);

        // TODO: Not used now
        final var rigLogic = RigLogic.read(buffer);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
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
        return new Component[0];
    }
}
