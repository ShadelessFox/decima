package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.command.AttributeChangeCommand;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.function.BiConsumer;

public class CoreTreeCellEditor implements TreeCellEditor, ActionListener {
    private final List<CellEditorListener> listeners = new ArrayList<>();
    private final CoreEditor editor;

    private EditorComponent component;

    public CoreTreeCellEditor(@NotNull CoreEditor editor) {
        this.editor = editor;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        component.customizeDecoration(tree, value, isSelected, expanded, leaf, row);

        return component;
    }

    @Override
    public Object getCellEditorValue() {
        return component.extractEditorValue();
    }

    @NotNull
    public ValueController<Object> getController() {
        return component.controller;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isCellEditable(EventObject event) {
        if (component != null) {
            component.dispose();
            component = null;
        }

        final TreePath path;

        if (event instanceof MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 3) {
                path = editor.getTree().getPathForLocation(e.getX(), e.getY());
            } else {
                return false;
            }
        } else {
            path = editor.getTree().getSelectionPath();
        }

        if (path != null && path.getLastPathComponent() instanceof CoreNodeObject node) {
            final var manager = (ValueManager<Object>) ValueRegistry.getInstance().findManager(
                node.getValue(),
                node.getType(),
                editor.getInput().getProject().getContainer().getType()
            );

            if (manager != null) {
                final ValueController<Object> controller = new EditorController(manager, node);
                final ValueEditor<Object> editor = manager.createEditor(controller);

                if (editor != null) {
                    component = new EditorComponent(controller, editor);
                }
            }
        }

        return component != null;
    }

    @Override
    public boolean shouldSelectCell(EventObject event) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireChangeEvent(CellEditorListener::editingStopped);
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireChangeEvent(CellEditorListener::editingCanceled);
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        listeners.add(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        stopCellEditing();
    }

    private void fireChangeEvent(@NotNull BiConsumer<CellEditorListener, ChangeEvent> consumer) {
        if (listeners.isEmpty()) {
            return;
        }

        final ChangeEvent event = new ChangeEvent(this);

        for (CellEditorListener listener : listeners) {
            consumer.accept(listener, event);
        }
    }

    private class EditorController implements ValueController<Object> {
        private final ValueManager<Object> manager;
        private final CoreNodeObject node;

        public EditorController(@NotNull ValueManager<Object> manager, @NotNull CoreNodeObject node) {
            this.manager = manager;
            this.node = node;
        }

        @NotNull
        @Override
        public EditType getEditType() {
            return EditType.INLINE;
        }

        @NotNull
        @Override
        public ValueManager<Object> getValueManager() {
            return manager;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public RTTIType<Object> getValueType() {
            return (RTTIType<Object>) node.getType();
        }

        @NotNull
        @Override
        public String getValueLabel() {
            return node.getLabel();
        }

        @NotNull
        @Override
        public Project getProject() {
            return editor.getInput().getProject();
        }

        @NotNull
        @Override
        public Object getValue() {
            return node.getValue();
        }

        @Override
        public void setValue(@NotNull Object newValue) {
            final Object oldValue = getValue();

            if (!newValue.equals(oldValue)) {
                editor.getCommandManager().add(new AttributeChangeCommand(editor.getTree(), node, oldValue, newValue));
            }
        }
    }

    private class EditorComponent extends JComponent {
        private final ValueController<Object> controller;
        private final ValueEditor<Object> editor;
        private final ColoredComponent decoration;

        public EditorComponent(@NotNull ValueController<Object> controller, @NotNull ValueEditor<Object> editor) {
            this.controller = controller;
            this.editor = editor;
            this.decoration = new ColoredComponent();

            final JComponent component = editor.createComponent();
            component.setBorder(UIManager.getBorder("Tree.editorBorder"));

            setBackground(UIManager.getColor("Tree.selectionInactiveBackground"));
            setLayout(new BorderLayout());
            add(decoration, BorderLayout.WEST);
            add(component, BorderLayout.CENTER);

            editor.addActionListener(CoreTreeCellEditor.this);
            editor.setEditorValue(controller.getValue());
        }

        @SuppressWarnings("unchecked")
        public void customizeDecoration(@NotNull JTree tree, @NotNull Object value, boolean selected, boolean expanded, boolean leaf, int row) {
            final ColoredTreeCellRenderer<Object> renderer = (ColoredTreeCellRenderer<Object>) tree.getCellRenderer();

            decoration.clear();
            decoration.setFont(renderer.getFont());
            decoration.setLeadingIcon(renderer.getIcon(tree, value, selected, expanded, false, leaf, row));
            decoration.append(controller.getValueLabel(), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
            decoration.append(" = ", TextAttributes.REGULAR_ATTRIBUTES);
            decoration.append("{%s} ".formatted(RTTITypeRegistry.getFullTypeName(controller.getValueType())), TextAttributes.GRAYED_ATTRIBUTES);
        }

        @NotNull
        private Object extractEditorValue() {
            return editor.getEditorValue();
        }

        private void dispose() {
            editor.removeActionListener(CoreTreeCellEditor.this);
        }
    }
}
