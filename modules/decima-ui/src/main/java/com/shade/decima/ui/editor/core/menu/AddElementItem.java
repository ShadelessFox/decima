package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.command.ElementAddCommand;
import com.shade.decima.ui.editor.core.command.ElementAddCommand.Operation;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Add Element\u2026", icon = "Action.addElementIcon", keystroke = "alt INSERT", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT_ARRAY, order = 1000)
public class AddElementItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        new EditDialog(editor, node).showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject obj
            && obj.getType() instanceof RTTITypeArray<?>;
    }

    private static class EditDialog extends BaseDialog {
        private final CoreEditor editor;
        private final CoreNodeObject node;

        private JComboBox<RTTIType<?>> typeCombo;
        private JSpinner indexSpinner;

        public EditDialog(@NotNull CoreEditor editor, @NotNull CoreNodeObject node) {
            super("Add new element to '%s'".formatted(node.getLabel()));
            this.editor = editor;
            this.node = node;
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final RTTITypeArray<?> type = (RTTITypeArray<?>) node.getType();
            final Object array = node.getValue();
            final int length = type.length(array);

            final RTTIType<?>[] types = findDescendantTypes(editor.getInput().getProject().getRTTIFactory(), type.getComponentType());
            typeCombo = new JComboBox<>(types);
            typeCombo.setEnabled(types.length > 1);
            typeCombo.setSelectedItem(type.getComponentType());
            typeCombo.setRenderer(new ColoredListCellRenderer<>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends RTTIType<?>> list, @NotNull RTTIType<?> value, int index, boolean selected, boolean focused) {
                    appendFullTypeName(value);
                }

                private void appendFullTypeName(@NotNull RTTIType<?> type) {
                    if (type instanceof RTTITypeParameterized<?, ?> parameterized) {
                        append(type.getTypeName(), TextAttributes.GRAYED_ATTRIBUTES);
                        append("<", TextAttributes.GRAYED_ATTRIBUTES);
                        appendFullTypeName(parameterized.getComponentType());
                        append(">", TextAttributes.GRAYED_ATTRIBUTES);
                    } else {
                        append(type.getTypeName(), TextAttributes.REGULAR_ATTRIBUTES);
                    }
                }
            });

            indexSpinner = new JSpinner(new SpinnerNumberModel(length, 0, length, 1));
            indexSpinner.setEnabled(length > 0);

            final JPanel panel = new JPanel();
            panel.setLayout(new MigLayout("ins 0", "[fill][grow,fill,200lp]"));

            final JLabel typeLabel = Mnemonic.resolve(new JLabel("&Type:"));
            typeLabel.setLabelFor(typeCombo);

            final JLabel indexLabel = Mnemonic.resolve(new JLabel("I&ndex"));
            indexLabel.setLabelFor(indexSpinner);

            panel.add(typeLabel);
            panel.add(typeCombo, "wrap");

            panel.add(indexLabel);
            panel.add(indexSpinner, "wrap");

            return panel;
        }

        @Override
        protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
            if (descriptor == BUTTON_OK) {
                final RTTIType<?> type = typeCombo.getItemAt(typeCombo.getSelectedIndex());
                final int index = (int) indexSpinner.getValue();
                editor.getCommandManager().add(new ElementAddCommand(Operation.ADD, editor.getTree(), node, type.instantiate(), index));
            }

            super.buttonPressed(descriptor);
        }

        @NotNull
        private RTTIType<?>[] findDescendantTypes(@NotNull RTTIFactory factory, @NotNull RTTIType<?> parent) {
            final List<RTTIType<?>> children = new ArrayList<>();

            if (parent instanceof RTTIClass cls) {
                for (RTTIType<?> type : factory) {
                    if (type instanceof RTTIClass child && child.isInstanceOf(cls)) {
                        children.add(child);
                    }
                }
            } else if (parent instanceof RTTITypeParameterized<?, ?> parameterized) {
                for (RTTIType<?> type : findDescendantTypes(factory, parameterized.getComponentType())) {
                    children.add(parameterized.clone(type));
                }
            } else {
                children.add(parent);
            }

            children.sort(Comparator.comparing(RTTIType::getFullTypeName));

            return children.toArray(RTTIType<?>[]::new);
        }
    }
}
