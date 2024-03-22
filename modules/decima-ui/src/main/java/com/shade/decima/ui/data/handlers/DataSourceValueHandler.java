package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "data-source", name = "Data Source", value = {
    @Selector(type = @Type(type = HwDataSource.class))
})
public class DataSourceValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIObject) value).<HwDataSource>cast().getLocation();
    }
}
