package com.shade.decima.ui.menu;

import com.shade.decima.model.util.LazyWithMetadata;
import com.shade.decima.model.util.NotNull;

import java.util.List;

public interface MenuItemProvider {
    @NotNull
    List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext context);
}
