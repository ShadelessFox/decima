package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeContainer;
import com.shade.decima.rtti.objects.RTTICollection;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayValueHandler implements ValueCollectionHandler<RTTICollection<?>, ArrayValueHandler.IndexedValue> {
    public static final ArrayValueHandler INSTANCE = new ArrayValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    @NotNull
    @Override
    public Collection<IndexedValue> getChildren(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection) {
        final List<IndexedValue> children = new ArrayList<>(collection.size());
        for (int i = 0; i < collection.size(); i++) {
            children.add(new IndexedValue(i, collection.get(i)));
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull IndexedValue value) {
        return String.valueOf(value.index());
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull IndexedValue value) {
        return value.value();
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull IndexedValue value) {
        return ((RTTITypeContainer<?>) type).getContainedType();
    }

    protected static record IndexedValue(int index, @NotNull Object value) {
    }
}
