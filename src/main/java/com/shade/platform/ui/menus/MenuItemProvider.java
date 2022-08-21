package com.shade.platform.ui.menus;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.util.NotNull;

import java.util.List;

public interface MenuItemProvider {
    @NotNull
    List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext context);
}
