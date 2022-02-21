package com.shade.decima.ui.handlers.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handlers.ValueHandler;

public class StringValueHandler implements ValueHandler {
    public static final StringValueHandler INSTANCE = new StringValueHandler();

    private StringValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        String result = (String) value;
        result = replaceCharEscape(result, "\r", "\\r");
        result = replaceCharEscape(result, "\n", "\\n");
        result = replaceCharEscape(result, "\t", "\\t");
        result = replaceCharEscape(result, "\"", "\\\"");

        return "<font color=#067d17>\"%s\"</font>".formatted(result);
    }

    @NotNull
    private String replaceCharEscape(@NotNull String value, @NotNull String target, @NotNull String replacement) {
        return value.replace(target, "<font color=#0037a6>%s</font>".formatted(replacement));
    }
}
