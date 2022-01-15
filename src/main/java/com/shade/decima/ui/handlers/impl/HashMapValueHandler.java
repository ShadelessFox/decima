package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HashMapValueHandler implements ValueCollectionHandler<Object[], RTTIObject> {
    public static final HashMapValueHandler INSTANCE = new HashMapValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    @NotNull
    @Override
    public Collection<RTTIObject> getChildren(@NotNull RTTIType<?> type, @NotNull Object[] objects) {
        final List<RTTIObject> children = new ArrayList<>(objects.length);
        for (Object object : objects) {
            children.add((RTTIObject) object);
        }
        return children;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull RTTIObject value) {
        return String.valueOf(value.<Object>getMemberValue("Key"));
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull RTTIObject value) {
        return value.getMemberValue("Value");
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object[] object, @NotNull RTTIObject value) {
        return value.getMember("Value").type();
    }
}
