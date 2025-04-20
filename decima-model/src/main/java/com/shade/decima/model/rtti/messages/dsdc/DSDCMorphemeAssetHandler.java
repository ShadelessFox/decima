package com.shade.decima.model.rtti.messages.dsdc;

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
    @Type(name = "MorphemeAsset", game = GameType.DSDC),
})
public class DSDCMorphemeAssetHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Data", BufferUtils.getBytes(buffer, buffer.getInt()));
        object.set("AssetID", buffer.getInt());
        object.set("AssetType", buffer.getInt());
        object.set("AssetSize", buffer.getLong());
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final byte[] data = object.get("Data");
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.putInt(object.i32("AssetID"));
        buffer.putInt(object.i32("AssetType"));
        buffer.putLong(object.i64("AssetSize"));
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
            new Component("AssetID", registry.find("uint32")),
            new Component("AssetType", registry.find("int32")),
            new Component("AssetSize", registry.find("int64"))
        };
    }
}
