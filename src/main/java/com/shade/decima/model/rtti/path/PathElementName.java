package com.shade.decima.model.rtti.path;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

public record PathElementName(@NotNull RTTITypeClass.Member member) implements PathElement {
    @NotNull
    @Override
    public Object get(@NotNull Object object) {
        return ((RTTIObject) object).get(member);
    }

    @Override
    public void set(@NotNull Object object, @NotNull Object value) {
        ((RTTIObject) object).set(member, value);
    }
}
