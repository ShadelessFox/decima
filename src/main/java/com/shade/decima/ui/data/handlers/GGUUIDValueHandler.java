package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class GGUUIDValueHandler implements ValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        final RTTIObject object = (RTTIObject) value;
        final String uuid = "{%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x}".formatted(
            object.i8("Data3"),
            object.i8("Data2"),
            object.i8("Data1"),
            object.i8("Data0"),
            object.i8("Data5"),
            object.i8("Data4"),
            object.i8("Data7"),
            object.i8("Data6"),
            object.i8("Data8"),
            object.i8("Data9"),
            object.i8("Data10"),
            object.i8("Data11"),
            object.i8("Data12"),
            object.i8("Data13"),
            object.i8("Data14"),
            object.i8("Data15")
        );

        component.append(uuid, TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.uuidIcon");
    }
}
