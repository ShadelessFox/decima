package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.action.Actions;
import com.shade.decima.ui.editor.PropertyEditorPane;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

public class EditorsPane extends JTabbedPane {
    private PropertyEditorPane focusedEditor;
    private PropertyEditorPane activeEditor;

    public EditorsPane() {
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) this::removeTabAt);

        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        setFocusable(false);

        addChangeListener(e -> setActiveEditor((PropertyEditorPane) getSelectedComponent()));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final int index = indexAtLocation(e.getX(), e.getY());
                if (SwingUtilities.isRightMouseButton(e) && index >= 0) {
                    focusedEditor = (PropertyEditorPane) getComponentAt(index);
                    final JPopupMenu menu = new JPopupMenu();
                    Actions.contribute(menu, "popup:editor");
                    menu.show(EditorsPane.this, e.getX(), e.getY());
                }
            }
        });
    }

    public void showEditor(@NotNull NavigatorFileNode node) {
        showEditor(node, true);
    }

    public void showEditor(@NotNull NavigatorFileNode node, boolean reveal) {
        PropertyEditorPane pane = findEditor(node);

        if (pane == null) {
            pane = new PropertyEditorPane(node);
            addTab(node.toString(), pane);
            setSelectedComponent(pane);
            UIUtils.minimizePanel(pane, false);
        }

        if (reveal) {
            setSelectedComponent(pane);
            pane.getPropertiesTree().requestFocusInWindow();
        }
    }

    @Nullable
    public PropertyEditorPane findEditor(@NotNull NavigatorFileNode node) {
        final Packfile packfile = UIUtils.getPackfile(node);

        for (int i = 0; i < getTabCount(); i++) {
            final PropertyEditorPane editor = (PropertyEditorPane) getComponentAt(i);

            if (editor.getPackfile() == packfile && editor.getNode().getHash() == node.getHash()) {
                return editor;
            }
        }

        return null;
    }

    public void closeEditor(@NotNull NavigatorFileNode node) {
        final Packfile packfile = UIUtils.getPackfile(node);

        for (int i = 0; i < getTabCount(); i++) {
            final PropertyEditorPane editor = (PropertyEditorPane) getComponentAt(i);

            if (editor.getPackfile() == packfile && editor.getNode().getHash() == node.getHash()) {
                removeTabAt(i);
                return;
            }
        }
    }

    @Nullable
    public PropertyEditorPane getFocusedEditor() {
        return focusedEditor;
    }

    public void setFocusedEditor(@Nullable PropertyEditorPane focusedEditor) {
        this.focusedEditor = focusedEditor;
    }

    @Nullable
    public PropertyEditorPane getActiveEditor() {
        return activeEditor;
    }

    public void setActiveEditor(@Nullable PropertyEditorPane activeEditor) {
        final PropertyEditorPane oldActiveEditor = this.activeEditor;
        this.activeEditor = activeEditor;
        this.focusedEditor = activeEditor;
        this.firePropertyChange("activeEditor", oldActiveEditor, activeEditor);
    }
}
