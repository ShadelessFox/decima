package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.CoreValueController;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Edit &Modal\u2026", icon = "Editor.editModalIcon", keystroke = "ctrl ENTER", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 2000)
public class ModalEditItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final ValueManager<Object> manager = Objects.requireNonNull(ValueRegistry.getInstance().findManager(
            node.getValue(),
            node.getType(),
            editor.getInput().getProject().getContainer().getType()
        ));

        final ValueController<Object> controller = new CoreValueController(editor, manager, node, ValueController.EditType.DIALOG);
        final EditDialog dialog = new EditDialog(controller);

        dialog.showDialog(Application.getFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);

        if (editor != null && selection instanceof CoreNodeObject node) {
            final ValueManager<Object> manager = ValueRegistry.getInstance().findManager(
                node.getValue(),
                node.getType(),
                editor.getInput().getProject().getContainer().getType()
            );

            return manager != null && manager.canEdit(ValueController.EditType.DIALOG);
        }

        return false;
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        return editor != null && !editor.getTree().isEditing();
    }

    private static class EditDialog extends BaseEditDialog {
        private final ValueController<Object> controller;
        private final ValueEditor<Object> editor;

        public EditDialog(@NotNull ValueController<Object> controller) {
            super("Edit '%s'".formatted(controller.getValueLabel()));
            this.controller = controller;
            this.editor = controller.getValueManager().createEditor(controller);
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
