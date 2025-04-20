package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.MutableValueController.EditType;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Edit &Modal\u2026", icon = "Action.editModalIcon", keystroke = "ctrl ENTER", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 2000)
public class ModalEditItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final MutableValueController<Object> controller = Objects.requireNonNull(editor.getValueController(EditType.DIALOG));
        final ValueManager<Object> manager = Objects.requireNonNull(ValueRegistry.getInstance().findManager(controller));
        final EditDialog dialog = new EditDialog(manager, controller);

        dialog.showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final ValueController<Object> controller = editor.getValueController(EditType.DIALOG);

        if (!editor.getTree().isEditing() && controller != null) {
            final ValueManager<Object> manager = ValueRegistry.getInstance().findManager(controller);
            return manager != null && manager.canEdit(EditType.DIALOG);
        }

        return false;
    }

    private static class EditDialog extends BaseEditDialog {
        private final MutableValueController<Object> controller;
        private final ValueEditor<Object> editor;

        public EditDialog(@NotNull ValueManager<Object> manager, @NotNull MutableValueController<Object> controller) {
            super("Edit '%s'".formatted(controller.getValueLabel()));
            this.controller = controller;
            this.editor = manager.createEditor(controller);
        }

        @Override
        protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
            if (descriptor == BUTTON_OK) {
                controller.setValue(editor.getEditorValue());
            }

            super.buttonPressed(descriptor);
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final JComponent component = editor.createComponent();

            editor.setEditorValue(controller.getValue());

            if (component instanceof Scrollable) {
                return new JScrollPane(component);
            } else {
                return component;
            }
        }

        @Override
        protected boolean isComplete() {
            return true;
        }
    }
}
