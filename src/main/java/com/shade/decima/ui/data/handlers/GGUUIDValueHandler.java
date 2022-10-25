package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(@Type(name = "GGUUID"))
public class GGUUIDValueHandler implements ValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("{%s}".formatted(getString(type, value)), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.uuidIcon");
    }

    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject o = (RTTIObject) value;

        return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".formatted(
            o.i8("Data3"), o.i8("Data2"), o.i8("Data1"), o.i8("Data0"),
            o.i8("Data5"), o.i8("Data4"),
            o.i8("Data7"), o.i8("Data6"),
            o.i8("Data8"), o.i8("Data9"),
            o.i8("Data10"), o.i8("Data11"), o.i8("Data12"), o.i8("Data13"), o.i8("Data14"), o.i8("Data15")
        );
    }
}
