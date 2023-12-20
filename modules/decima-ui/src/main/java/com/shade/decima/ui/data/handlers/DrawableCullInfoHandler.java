package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.util.Map;
import java.util.StringJoiner;

@ValueHandlerRegistration(id = "drawableCullInfo", name = "Cull Info", value = {
    @Selector(type = @Type(name = "DrawableCullInfo")),
})
public class DrawableCullInfoHandler extends ObjectValueHandler {
    private static final Map<Integer, String> KNOWN_FLAGS = Map.of(
        32, "Cast shadows"
    );

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            int flags = ((RTTIObject) value).i32("Flags");
            final StringJoiner joiner = new StringJoiner(", ");

            for (Map.Entry<Integer, String> flag : KNOWN_FLAGS.entrySet()) {
                if ((flags & flag.getKey()) != 0) {
                    joiner.add(flag.getValue());
                    flags &= ~flag.getKey();
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
}
