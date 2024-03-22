package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.hzd.HZDTextureHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.ds.DSTextureData;
import com.shade.decima.model.rtti.types.ds.DSTextureHeader;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Texture", game = GameType.DS),
    @Type(name = "Texture", game = GameType.DSDC)
})
public class DSTextureHandler extends HZDTextureHandler {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Header", DSTextureHeader.read(registry, buffer));
        object.set("Data", DSTextureData.read(registry, buffer));
    }
}
