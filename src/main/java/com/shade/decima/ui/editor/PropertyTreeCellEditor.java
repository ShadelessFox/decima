package com.shade.decima.ui.editor;

import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueEditorProvider;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class PropertyTreeCellEditor extends DefaultCellEditor implements ActionListener {
    private PropertyTreeNode node;
    private ValueEditor editor;
    private JComponent editorComponent;

    public PropertyTreeCellEditor() {
        super(new JTextField("<placeholder>"));
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        if (editor != null) {
            editor.removeActionListener(editorComponent, this);
            editorComponent = null;
        }

        final PropertyTreeNode node = (PropertyTreeNode) value;
        final ValueEditor editor = ValueEditorProvider.findValueEditor(node.getType());

        if (editor == null) {
            return null;
        }

        this.node = node;
        this.editor = editor;
        this.editorComponent = editor.createComponent(node.getType());

        editor.addActionListener(editorComponent, this);
        editor.setEditorValue(editorComponent, node.getType(), node.getUserObject());

        return editorComponent;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getEditorValue(editorComponent, node.getType());
    }

    @Override
    public boolean isCellEditable(EventObject eventObject) {
        boolean editable = super.isCellEditable(eventObject);

        if (editable && eventObject instanceof MouseEvent event && eventObject.getSource() instanceof JTree tree) {
            final TreePath path = tree.getClosestPathForLocation(event.getX(), event.getY());
            if (path != null && path.getLastPathComponent() instanceof PropertyTreeNode node) {
                return ValueEditorProvider.findValueEditor(node.getType()) != null;
            }
        }

        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        stopCellEditing();
    }
}
