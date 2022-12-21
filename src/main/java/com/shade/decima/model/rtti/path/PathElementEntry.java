package com.shade.decima.model.rtti.path;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

public record PathElementEntry(@NotNull RTTIObject entry) implements PathElement {
    @NotNull
    @Override
    public Object get(@NotNull Object object) {
        return entry;
    }

    @Override
    public void set(@NotNull Object object, @NotNull Object value) {
        throw new NotImplementedException();
    }
}
