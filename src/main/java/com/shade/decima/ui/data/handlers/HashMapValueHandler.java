package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HashMapValueHandler implements ValueHandlerCollection<Object[], RTTIObject> {
    public static final HashMapValueHandler INSTANCE = new HashMapValueHandler();

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append("size = " + ((Object[]) value).length, TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }

    @NotNull
    @Override
    public Collection<RTTIObject> getChildren(@NotNull RTTIType<?> type, @NotNull Object[] values) {
        final List<RTTIObject> children = new ArrayList<>(values.length);
        for (Object object : values) {
            children.add((RTTIObject) object);
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull RTTIObject value) {
        return String.valueOf(value.<Object>get("Key"));
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull RTTIObject value) {
        return value.get("Value");
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull RTTIObject value) {
        return value.getType().getMember("Value").type();
    }
}
