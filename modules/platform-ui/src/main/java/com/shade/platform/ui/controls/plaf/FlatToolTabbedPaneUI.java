package com.shade.platform.ui.controls.plaf;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.shade.platform.ui.controls.ToolTabbedPane;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class FlatToolTabbedPaneUI extends FlatTabbedPaneUI {
    public FlatToolTabbedPaneUI() {
    }

    @NotNull
    public static ComponentUI createUI(@NotNull JComponent c) {
        return new FlatToolTabbedPaneUI();
    }

    @Override
    protected LayoutManager createLayoutManager() {
        return new CompactTabbedPaneLayout();
    }

    @Override
    protected MouseListener createMouseListener() {
        final MouseListener delegate = super.createMouseListener();

        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                delegate.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (tabPane.isEnabled()) {
                    final int tabIndex = tabForCoordinate(tabPane, e.getX(), e.getY());

                    if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex) && tabIndex == tabPane.getSelectedIndex()) {
                        ((ToolTabbedPane) tabPane).minimizePane();
                        return;
                    }
                }

                delegate.mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                delegate.mouseReleased(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                delegate.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                delegate.mouseExited(e);
            }
        };
    }

    private class CompactTabbedPaneLayout extends FlatTabbedPaneLayout {
        @Override
        protected boolean isContentEmpty() {
            return tabPane.getSelectedIndex() < 0 || super.isContentEmpty();
        }
    }
}
