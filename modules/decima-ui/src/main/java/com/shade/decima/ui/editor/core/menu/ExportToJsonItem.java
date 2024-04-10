package com.shade.decima.ui.editor.core.menu;

import com.google.gson.stream.JsonWriter;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.editor.core.CoreNodeFile;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Export to JSON\u2026", icon = "Action.exportIcon", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 4000)
public class ExportToJsonItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final JFileChooser chooser = new FileChooser();
        chooser.setDialogTitle("Save as");
        chooser.setFileFilter(new FileExtensionFilter("JSON Files", "json"));
        chooser.setSelectedFile(new File("exported.json"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(chooser.getSelectedFile().toPath()))) {
            writer.setLenient(false);
            writer.setIndent("\t");

            final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);

            if (selection instanceof CoreNodeFile node) {
                writer.beginArray();

                for (RTTIObject entry : node.getCoreFile().objects()) {
                    RTTIUtils.serialize(entry, entry.type(), writer);
                }

                writer.endArray();
            } else {
                final CoreNodeObject node = (CoreNodeObject) selection;
                RTTIUtils.serialize(node.getValue(), node.getType(), writer);
            }
        } catch (IOException e) {
            UIUtils.showErrorDialog(e, "Can't export as JSON");
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);
        return selection instanceof CoreNodeFile
            || selection instanceof CoreNodeObject;
    }
}
