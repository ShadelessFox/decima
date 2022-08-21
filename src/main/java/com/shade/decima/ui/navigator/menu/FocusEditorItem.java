package com.shade.decima.ui.navigator.menu;

import com.shade.decima.ui.Application;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Focus Editor", keystroke = "ESCAPE", group = CTX_MENU_NAVIGATOR_GROUP_GENERAL, order = 0)
public class FocusEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = Application.getFrame().getEditorManager().getActiveEditor();

        if (editor != null) {
            editor.setFocus();
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return Application.getFrame().getEditorManager().getActiveEditor() != null;
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return false;
    }
}
