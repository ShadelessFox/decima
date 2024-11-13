package com.shade.decima.hrzr.rtti;

import com.shade.decima.rtti.TypeId;
import com.shade.util.NotNull;

public record HRZRTypeId(long hash) implements TypeId {
    @NotNull
    public static TypeId of(long hash) {
        return new HRZRTypeId(hash);
    }
}
