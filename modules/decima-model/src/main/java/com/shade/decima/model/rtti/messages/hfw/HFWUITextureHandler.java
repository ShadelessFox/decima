package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "UITexture", game = GameType.HFW),
})
public class HFWUITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var flag = buffer.get() != 0;

        if (flag) {
            throw new NotImplementedException();
        }

        final var smallTextureSize = buffer.getInt();
        final var bigTextureSize = buffer.getInt();

        if (smallTextureSize > 0) {
            object.set("SmallTextureData", BufferUtils.getBytes(buffer, smallTextureSize));
        }

        if (bigTextureSize > 0) {
            object.set("BigTextureData", BufferUtils.getBytes(buffer, bigTextureSize));
        }
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
            new Component("SmallTextureData", registry.find("Array<uint8>")),
            new Component("BigTextureData", registry.find("Array<uint8>"))
        };
    }
}
