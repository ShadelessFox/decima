package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HZDLocalizedText implements HwLocalizedText {
    @RTTIField(type = @Type(name = "String"))
    public String text;

    @NotNull
    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final var object = new HZDLocalizedText();
        object.text = IOUtils.getString(buffer, buffer.getShort());

        return new RTTIObject(registry.find(HZDLocalizedText.class), object);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final byte[] text = this.text.getBytes(StandardCharsets.UTF_8);

        buffer.putShort((short) text.length);
        buffer.put(text);
    }

    @Override
    public int getSize() {
        return 5 + text.getBytes(StandardCharsets.UTF_8).length;
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }
}
