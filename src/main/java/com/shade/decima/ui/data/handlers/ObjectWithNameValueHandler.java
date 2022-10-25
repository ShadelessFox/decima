package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public class ObjectWithNameValueHandler extends ObjectValueHandler {
    public static final ObjectWithNameValueHandler INSTANCE = new ObjectWithNameValueHandler();

    private ObjectWithNameValueHandler() {
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(((RTTIObject) value).str("Name"), TextAttributes.REGULAR_ATTRIBUTES);
    }
}
