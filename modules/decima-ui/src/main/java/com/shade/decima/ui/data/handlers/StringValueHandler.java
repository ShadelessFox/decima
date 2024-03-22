package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(value = {
    @Selector(type = @Type(type = String.class))
})
public class StringValueHandler implements ValueHandler {
    private static final String STRING_QUOTE = "\"";
    private static final String[][] STRING_ESCAPE_CHARACTERS = {
        {"\n", "\\n"},
        {"\r", "\\r"},
        {"\t", "\\t"},
        {"\"", "\\\""}
    };

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final String text = (String) value;

            component.append(STRING_QUOTE, CommonTextAttributes.STRING_TEXT_ATTRIBUTES);

            for (int start = 0; start < text.length(); ) {
                boolean matches = false;

                for (String[] escape : STRING_ESCAPE_CHARACTERS) {
                    final String expected = escape[0];
                    final String replacement = escape[1];
                    final int index = text.indexOf(expected, start);

                    if (index >= 0) {
                        component.append(text.substring(start, index), CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
                        component.append(replacement, CommonTextAttributes.STRING_ESCAPE_ATTRIBUTES);

                        start = index + expected.length();
                        matches = true;
                    }
                }

                if (!matches) {
                    component.append(text.substring(start) + STRING_QUOTE, CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
                    return;
                }
            }

            component.append(STRING_QUOTE, CommonTextAttributes.STRING_TEXT_ATTRIBUTES);

        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.stringIcon");
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return value.toString();
    }
}
