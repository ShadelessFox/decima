package com.shade.decima.model.rtti.messages.shared;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "ShaderResource", game = GameType.HFW),
})
public class ShaderResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var size = buffer.getInt();
        object.set("Hash", registry.find("MurmurHashValue").read(registry, buffer));
        object.set("Data", BufferUtils.getBytes(buffer, size));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final byte[] data = object.get("Data");
        buffer.putInt(data.length);
        final RTTIObject hash = object.obj("Hash");
        hash.type().write(registry, buffer, hash);
        buffer.put(data);
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return 20 + object.<byte[]>get("Data").length;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Data", registry.find("Array<uint8>")),
            new Component("Hash", registry.find("MurmurHashValue"))
        };
    }
}
