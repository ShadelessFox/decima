package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueHandler;

import java.awt.*;

public class StringValueHandler implements ValueHandler {
    public static final StringValueHandler INSTANCE = new StringValueHandler();

    private static final TextAttributes STRING_TEXT_ATTRIBUTES = new TextAttributes(new Color(0x008000), TextAttributes.Style.PLAIN);
    private static final TextAttributes STRING_ESCAPE_ATTRIBUTES = new TextAttributes(new Color(0x000080), TextAttributes.Style.BOLD);

    private static final String[][] ESCAPE_CHARACTERS = {
        {"\b", "\\b"},
        {"\f", "\\f"},
        {"\n", "\\n"},
        {"\r", "\\r"},
        {"\s", "\\s"},
        {"\t", "\\t"}
    };

    private StringValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        final String text = (String) value;

        for (int start = 0; start < text.length(); start++) {
            boolean matches = false;

            for (String[] escape : ESCAPE_CHARACTERS) {
                final String expected = escape[0];
                final String replacement = escape[1];
                final int index = text.indexOf(expected, start);

                if (index >= 0) {
                    component.append(text.substring(start, index), STRING_TEXT_ATTRIBUTES);
                    component.append(replacement, STRING_ESCAPE_ATTRIBUTES);

                    start = index + expected.length();
                    matches = true;
                }
            }

            if (!matches) {
                component.append(text.substring(start), STRING_TEXT_ATTRIBUTES);
                return;
            }
        }
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
