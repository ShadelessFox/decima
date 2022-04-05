package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.action.Actions;
import com.shade.decima.ui.editor.EditorPane;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

public class EditorsPane extends JTabbedPane {
    private EditorPane focusedEditor;
    private EditorPane activeEditor;

    public EditorsPane() {
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) this::removeTabAt);

        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        addChangeListener(e -> setActiveEditor((EditorPane) getSelectedComponent()));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final int index = indexAtLocation(e.getX(), e.getY());
                if (SwingUtilities.isRightMouseButton(e) && index >= 0) {
                    focusedEditor = (EditorPane) getComponentAt(index);
                    final JPopupMenu menu = new JPopupMenu();
                    Actions.contribute(menu, "popup:editor");
                    menu.show(EditorsPane.this, e.getX(), e.getY());
                }
            }
        });
    }

    public void showEditor(@NotNull NavigatorFileNode node) {
        for (int i = 0; i < getTabCount(); i++) {
            final EditorPane editor = (EditorPane) getComponentAt(i);

            if (editor.getNode() == node) {
                setSelectedComponent(editor);
                requestFocusInWindow();
                return;
            }
        }

        final EditorPane pane = new EditorPane(UIUtils.getProject(node), node);

        addTab(node.toString(), pane);
        setSelectedComponent(pane);
        requestFocusInWindow();
    }

    public void closeEditor(@NotNull NavigatorFileNode node) {
        for (int i = 0; i < getTabCount(); i++) {
            final EditorPane editor = (EditorPane) getComponentAt(i);

            if (editor.getNode() == node) {
                removeTabAt(i);
                return;
            }
        }
    }

    @Nullable
    public EditorPane getFocusedEditor() {
        return focusedEditor;
    }

    public void setFocusedEditor(@Nullable EditorPane focusedEditor) {
        this.focusedEditor = focusedEditor;
    }

    @Nullable
    public EditorPane getActiveEditor() {
        return activeEditor;
    }

    public void setActiveEditor(@Nullable EditorPane activeEditor) {
        this.activeEditor = activeEditor;
        this.focusedEditor = activeEditor;
    }
}
