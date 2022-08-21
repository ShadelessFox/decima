package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

public class GGUUIDValueHandler implements ValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        final RTTIObject object = (RTTIObject) value;
        final String uuid = "{%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x}".formatted(
            object.<Byte>get("Data3"),
            object.<Byte>get("Data2"),
            object.<Byte>get("Data1"),
            object.<Byte>get("Data0"),
            object.<Byte>get("Data5"),
            object.<Byte>get("Data4"),
            object.<Byte>get("Data7"),
            object.<Byte>get("Data6"),
            object.<Byte>get("Data8"),
            object.<Byte>get("Data9"),
            object.<Byte>get("Data10"),
            object.<Byte>get("Data11"),
            object.<Byte>get("Data12"),
            object.<Byte>get("Data13"),
            object.<Byte>get("Data14"),
            object.<Byte>get("Data15")
        );

        component.append(uuid, TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
