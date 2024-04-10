package com.shade.decima.model.rtti.messages.shared;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "ShaderResource", game = GameType.HFW),
})
public class ShaderResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var size = buffer.getInt();
        object.set("Hash", factory.find("MurmurHashValue").read(factory, reader, buffer));
        object.set("Data", BufferUtils.getBytes(buffer, size));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final byte[] data = object.get("Data");
        buffer.putInt(data.length);
        final RTTIObject hash = object.obj("Hash");
        hash.type().write(factory, buffer, hash);
        buffer.put(data);
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
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
