package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.messages.hzd.HZDLocalizedTextResourceHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.ds.DSLocalizedText;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedTextResource", game = GameType.DS),
    @Type(name = "LocalizedTextResource", game = GameType.DSDC)
})
public class DSLocalizedTextResourceHandler extends HZDLocalizedTextResourceHandler {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("Data", DSLocalizedText.read(registry, buffer));
    }
}
