package com.shade.platform.ui.editors.lazy;

import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.icons.LoadingIcon;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.HierarchyEvent;

public class LazyEditor implements Editor {
    private final LazyEditorInput input;
    private final JLabel placeholder;

    public LazyEditor(@NotNull LazyEditorInput input) {
        final LoadingIcon icon = new LoadingIcon();

        this.input = input;
        this.placeholder = new JLabel("Initializing\u2026", SwingConstants.CENTER);
        this.placeholder.setIcon(icon);

        final Timer timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
            placeholder.repaint();
            icon.advance();
        });

        placeholder.addHierarchyListener(e -> {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (placeholder.isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return placeholder;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @Override
    public void setFocus() {
        placeholder.requestFocusInWindow();
    }
}
