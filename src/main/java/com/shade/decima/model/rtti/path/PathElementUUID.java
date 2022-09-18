package com.shade.decima.model.rtti.path;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

public record PathElementUUID(@NotNull RTTIObject uuid) implements PathElement {
    @NotNull
    @Override
    public Object get(@NotNull Object object) {
        for (RTTIObject entry : ((CoreBinary) object).entries()) {
            if (entry.get("ObjectUUID").equals(uuid)) {
                return entry;
            }
        }

        throw new IllegalArgumentException("Couldn't find object");
    }

    @Override
    public void set(@NotNull Object object, @NotNull Object value) {
        throw new IllegalStateException("Not implemented");
    }
}
