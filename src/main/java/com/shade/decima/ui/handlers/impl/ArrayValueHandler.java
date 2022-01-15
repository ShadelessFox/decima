package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.types.RTTITypeArray;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayValueHandler implements ValueCollectionHandler<Object[], ArrayValueHandler.IndexedValue> {
    public static final ArrayValueHandler INSTANCE = new ArrayValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    @NotNull
    @Override
    public Collection<IndexedValue> getChildren(@NotNull RTTIType<?> type, @NotNull Object[] objects) {
        final List<IndexedValue> children = new ArrayList<>(objects.length);
        for (int i = 0; i < objects.length; i++) {
            children.add(new IndexedValue(i, objects[i]));
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull IndexedValue value) {
        return String.valueOf(value.index());
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull IndexedValue value) {
        return value.value();
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull IndexedValue value) {
        return ((RTTITypeArray<?>) type).getContainedType();
    }

    protected static record IndexedValue(int index, @NotNull Object value) {
    }
}
