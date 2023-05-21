package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.dialog.InspectValueDialog;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Inspect Value", icon = "Action.searchIcon", keystroke = "ctrl shift I", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 3000)
public class InspectValueItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final ValueController<Object> controller = Objects.requireNonNull(editor.getValueController());

        new InspectValueDialog<>(editor.getInput().getProject(), controller).showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.EDITOR_KEY) instanceof CoreEditor editor
            && editor.getValueController() != null;
    }
}
