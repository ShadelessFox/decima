package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.packfile.edit.FileChange;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "I&mport File\u2026", icon = "Action.importIcon", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 1000)
public class ImportContentsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final NavigatorFileNode file = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final FileChange change = new FileChange(chooser.getSelectedFile().toPath(), file.getHash());

        file.getPackfile().addChange(file.getPath(), change);
        EditorManager.getInstance().notifyInputChanged(new NodeEditorInputSimple(file));
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }
}
