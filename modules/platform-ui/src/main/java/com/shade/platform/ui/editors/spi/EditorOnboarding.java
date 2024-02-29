package com.shade.platform.ui.editors.spi;

import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public sealed interface EditorOnboarding {
    /**
     * Action (menu item) reference to be shown on the onboarding screen.
     *
     * @param id   identifier of the menu item, see {@link MenuItemRegistration#id()}
     * @param name custom name for the menu item. If {@code null}, the name of the menu item will be used
     */
    record Action(@NotNull String id, @Nullable String name) implements EditorOnboarding {
        public Action(@NotNull String id) {
            this(id, null);
        }
    }

    /**
     * Arbitrary text to be shown on the onboarding screen.
     *
     * @param text text to be shown
     */
    record Text(@NotNull String text) implements EditorOnboarding {
    }
}
