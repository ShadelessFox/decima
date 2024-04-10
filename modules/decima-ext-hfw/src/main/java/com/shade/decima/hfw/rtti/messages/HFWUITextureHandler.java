package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.hfw.rtti.types.HFWTexture;
import com.shade.decima.hfw.rtti.types.HFWTextureFrames;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "UITexture", game = GameType.HFW),
})
public class HFWUITextureHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var flag = buffer.get() != 0;
        final var smallTextureSize = buffer.getInt();
        final var bigTextureSize = buffer.getInt();

        if (flag) {
            if (smallTextureSize > 0) {
                object.set("SmallFramesData", HFWTextureFrames.read(factory, buffer));
            }
            if (bigTextureSize > 0) {
                object.set("BigFramesData", HFWTextureFrames.read(factory, buffer));
            }
        } else {
            if (smallTextureSize > 0) {
                object.set("SmallTextureData", HFWTexture.read(factory, buffer));
            }

            if (bigTextureSize > 0) {
                object.set("BigTextureData", HFWTexture.read(factory, buffer));
            }
        }
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
            new Component("SmallTextureData", factory.find(HFWTexture.class)),
            new Component("BigTextureData", factory.find(HFWTexture.class)),
            new Component("SmallFramesData", factory.find(HFWTextureFrames.class)),
            new Component("BigFramesData", factory.find(HFWTextureFrames.class)),
        };
    }
}
