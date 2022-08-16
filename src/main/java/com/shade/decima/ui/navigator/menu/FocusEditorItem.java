package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Focus Editor", keystroke = "ESCAPE", group = CTX_MENU_NAVIGATOR_GROUP_GENERAL, order = 0)
public class FocusEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = Application.getFrame().getEditorManager().getActiveEditor();

        if (editor != null) {
            editor.getController().getFocusComponent().requestFocusInWindow();
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
