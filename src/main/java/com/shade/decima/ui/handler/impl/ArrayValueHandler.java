package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueCollectionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayValueHandler implements ValueCollectionHandler<Object[], ArrayValueHandler.IndexedValue> {
    public static final ArrayValueHandler INSTANCE = new ArrayValueHandler();

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
    public Collection<IndexedValue> getChildren(@NotNull RTTIType<?> type, @NotNull Object[] values) {
        final List<IndexedValue> children = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            children.add(new IndexedValue(i, values[i]));
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull IndexedValue value) {
        return String.valueOf(value.index());
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull IndexedValue value) {
        return value.value();
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull IndexedValue value) {
        return ((RTTITypeContainer<?, ?>) type).getArgumentType();
    }

    protected static record IndexedValue(int index, @NotNull Object value) {
    }
}
