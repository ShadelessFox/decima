package com.shade.decima.ui.editor.core.menu.root;

import com.shade.decima.ui.editor.core.CoreNodeFile;
import com.shade.platform.ui.menus.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "&Group by Type", group = CTX_MENU_CORE_EDITOR_GROUP_APPEARANCE, order = 1000)
public class ToggleGroupingItem extends BaseToggleItem {
    public ToggleGroupingItem() {
        super(CoreNodeFile::isGroupingEnabled, CoreNodeFile::setGroupingEnabled);
    }
}
