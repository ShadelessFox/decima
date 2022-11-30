package com.shade.decima.model.rtti.path;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

public record PathElementField(@NotNull RTTIClass.Field<RTTIObject, Object> field) implements PathElement {
    @NotNull
    @Override
    public Object get(@NotNull Object object) {
        return ((RTTIObject) object).get(field);
    }

    @Override
    public void set(@NotNull Object object, @NotNull Object value) {
        ((RTTIObject) object).set(field, value);
    }
}
