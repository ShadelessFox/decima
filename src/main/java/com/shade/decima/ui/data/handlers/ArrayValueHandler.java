package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.path.PathElement;
import com.shade.decima.model.rtti.path.PathElementIndex;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayValueHandler implements ValueHandlerCollection<Object[], ArrayValueHandler.IndexedValue> {
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

    @NotNull
    @Override
    public PathElement getChildElement(@NotNull RTTIType<?> type, @NotNull Object[] values, @NotNull IndexedValue value) {
        return new PathElementIndex(value.index());
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.arrayIcon");
    }

    protected static record IndexedValue(int index, @NotNull Object value) {
    }
}
