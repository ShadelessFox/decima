package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class EnumValueHandler implements ValueHandler {
    public static final EnumValueHandler INSTANCE = new EnumValueHandler();

    private EnumValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append(String.valueOf(value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.enumIcon");
    }
}
