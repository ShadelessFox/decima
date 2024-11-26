package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.hash.CRC32C;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ValueHandlerRegistration(id = "KJPXPath", name = "KJP XPath", value = {
    @Selector(type = @Type(name = "KJPXPath"))
})
public class KJPXPathValueHandler extends ObjectValueHandler {
    private static final Map<Integer, String> LOOKUP;

    static {
        final String[] knownNames = {
            "continue",
            "new_game",
            "exit_game",
            "game_over",
            "update_info",
            "can_restart",
            "difficulty_select_title",
            "very_easy",
            "easy",
            "normal",
            "hard",
            "very_hard",
            "month",
            "date",
            "visible",
            "text()",
            "bool()",
            "!bool()",
            "int()",
            "float()",
            "object()",
            "isNull()",
            "!isNull()",
            "count()",
            "index()",
            "index1()",
            "exist()",
            "!exist()",
            "isEmpty()",
            "!isEmpty()",
        };

        LOOKUP = Stream.of(knownNames).collect(Collectors.toMap(
            name -> CRC32C.calculate(name.getBytes(StandardCharsets.UTF_8)),
            Function.identity()
        ));
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject object = (RTTIObject) value;
        final StringJoiner buffer = new StringJoiner("/");
        final int length = (int) object.i64("Union7");

        for (int i = 0; i < length; i++) {
            buffer.add(getElementName(object.i64("Union%d".formatted(i))));
        }

        return buffer.toString();
    }

    @NotNull
    private static String getElementName(long element) {
        final String name = LOOKUP.get((int) element);

        if (name != null) {
            return name;
        } else {
            return IOUtils.toHexDigits((int) element, ByteOrder.BIG_ENDIAN);
        }
    }
}
