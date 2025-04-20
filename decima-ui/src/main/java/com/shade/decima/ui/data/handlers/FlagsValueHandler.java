package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.StringJoiner;

public abstract class FlagsValueHandler extends NumberValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final StringJoiner joiner = new StringJoiner(", ");
            int flags = (int) value;

            for (int bit = Integer.highestOneBit(flags); bit > 0; bit >>= 1) {
                if ((flags & bit) == 0) {
                    continue;
                }

                final String name = getFlagName(bit);

                if (name != null) {
                    joiner.add(name);
                    flags &= ~bit;
                }
            }

            if (flags != 0) {
                joiner.add("Unknown flags: " + flags);
            }

            if (joiner.length() > 0) {
                component.append(joiner.toString(), TextAttributes.REGULAR_ATTRIBUTES);
            }
        };
    }

    @Nullable
    protected abstract String getFlagName(int flag);
}
