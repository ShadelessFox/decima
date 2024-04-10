package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.hzd.HZDTextureHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
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
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        object.set("Header", DSTextureHeader.read(factory, buffer));
        object.set("Data", DSTextureData.read(factory, reader, buffer));
    }
}
