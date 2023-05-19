package com.shade.decima.ui.editor.core.menu.root;

import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.platform.ui.menus.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "&Sort Lexicographically", group = CTX_MENU_CORE_EDITOR_GROUP_APPEARANCE, order = 2000)
public class ToggleSortingItem extends BaseToggleItem {
    public ToggleSortingItem() {
        super(CoreNodeBinary::isSortingEnabled, CoreNodeBinary::setSortingEnabled);
    }
}
