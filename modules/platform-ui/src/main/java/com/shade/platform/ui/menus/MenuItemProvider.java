package com.shade.platform.ui.menus;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.util.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

public interface MenuItemProvider {
    @NotNull
    static MenuItemRegistration createRegistration(@NotNull String parent, @NotNull String group) {
        return createRegistration(parent, group, "", "", "");
    }

    @NotNull
    static MenuItemRegistration createRegistration(@NotNull String parent, @NotNull String group, @NotNull String name, @NotNull String icon, @NotNull String keystroke) {
        return new MenuItemRegistration() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return MenuItemRegistration.class;
            }

            @Override
            public String parent() {
                return parent;
            }

            @Override
            public String id() {
                return "";
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String icon() {
                return icon;
            }

            @Override
            public String keystroke() {
                return keystroke;
            }

            @Override
            public String group() {
                return group;
            }

            @Override
            public int order() {
                return 0;
            }
        };
    }

    @NotNull
    List<LazyWithMetadata<MenuItem, MenuItemRegistration>> create(@NotNull MenuItemContext ctx);

    default boolean isInitializedOnDemand() {
        return false;
    }
}
