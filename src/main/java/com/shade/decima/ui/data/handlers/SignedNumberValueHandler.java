package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeNumber;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class SignedNumberValueHandler implements ValueHandler {
    public static final SignedNumberValueHandler INSTANCE = new SignedNumberValueHandler();

    private SignedNumberValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append(String.valueOf(value), TextAttributes.BLUE_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        if (((RTTITypeNumber<?>) type).isDecimal()) {
            return UIManager.getIcon("CoreEditor.decimalIcon");
        } else {
            return UIManager.getIcon("CoreEditor.integerIcon");
        }
    }
}
