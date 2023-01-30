package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.ds.DSLocalizedText;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedTextResource", game = GameType.DS),
    @Type(name = "LocalizedTextResource", game = GameType.DSDC)
})
public class DSLocalizedTextResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTITypeEnum languages = registry.find("ELanguage");
        final RTTIObject[] entries = new RTTIObject[languages.getConstants().length - 1];

        for (int i = 0; i < entries.length; i++) {
            entries[i] = DSLocalizedText.read(registry, buffer);
        }

        object.set("Entries", entries);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        for (RTTIObject entry : object.objs("Entries")) {
            entry.<HwLocalizedText>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return Arrays.stream(object.objs("Entries"))
            .map(RTTIObject::<HwLocalizedText>cast)
            .mapToInt(HwLocalizedText::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Entries", registry.find(HwLocalizedText[].class))
        };
    }
}
