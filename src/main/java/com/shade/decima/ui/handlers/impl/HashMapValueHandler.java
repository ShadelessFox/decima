package com.shade.decima.ui.handlers.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handlers.ValueCollectionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HashMapValueHandler implements ValueCollectionHandler<RTTICollection<?>, RTTIObject> {
    public static final HashMapValueHandler INSTANCE = new HashMapValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    @NotNull
    @Override
    public Collection<RTTIObject> getChildren(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection) {
        final List<RTTIObject> children = new ArrayList<>(collection.size());
        for (Object object : collection) {
            children.add((RTTIObject) object);
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull RTTIObject value) {
        return String.valueOf(value.<Object>get("Key"));
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull RTTIObject value) {
        return value.get("Value");
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull RTTICollection<?> collection, @NotNull RTTIObject value) {
        return value.getType().getMember("Value").type();
    }
}
