package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.hfw.HFWTexture;
import com.shade.decima.model.rtti.types.hfw.HFWTextureFrames;
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
        final var smallTextureSize = buffer.getInt();
        final var bigTextureSize = buffer.getInt();

        if (flag) {
            if (smallTextureSize > 0) {
                object.set("SmallFramesData", HFWTextureFrames.read(registry, buffer));
            }
            if (bigTextureSize > 0) {
                object.set("BigFramesData", HFWTextureFrames.read(registry, buffer));
            }
        } else {
            if (smallTextureSize > 0) {
                object.set("SmallTextureData", HFWTexture.read(registry, buffer));
            }

            if (bigTextureSize > 0) {
                object.set("BigTextureData", HFWTexture.read(registry, buffer));
            }
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
            new Component("SmallTextureData", registry.find(HFWTexture.class)),
            new Component("BigTextureData", registry.find(HFWTexture.class)),
            new Component("SmallFramesData", registry.find(HFWTextureFrames.class)),
            new Component("BigFramesData", registry.find(HFWTextureFrames.class)),
        };
    }
}
