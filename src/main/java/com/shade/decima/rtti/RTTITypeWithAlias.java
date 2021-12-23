package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;

public interface RTTITypeWithAlias<T> extends RTTIType<T> {
    @NotNull
    String[] getAliases();
}
