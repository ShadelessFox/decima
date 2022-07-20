package com.shade.decima.ui.editor.lazy;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorController;
import com.shade.decima.ui.editor.EditorInput;
import com.shade.decima.ui.icon.LoadingIcon;

import javax.swing.*;
import java.awt.event.HierarchyEvent;

public class LazyEditor implements Editor {
    private final LazyEditorInput input;
    private final EditorController controller;
    private final JLabel placeholder;

    public LazyEditor(@NotNull LazyEditorInput input) {
        final LoadingIcon icon = new LoadingIcon();

        this.input = input;
        this.controller = new Controller();
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

    @NotNull
    @Override
    public EditorController getController() {
        return controller;
    }

    private class Controller implements EditorController {
        @Nullable
        @Override
        public RTTIType<?> getSelectedType() {
            return null;
        }

        @Nullable
        @Override
        public Object getSelectedValue() {
            return null;
        }

        @Override
        public void setSelectedValue(@Nullable Object value) {
            // not implemented
        }

        @NotNull
        @Override
        public JComponent getFocusComponent() {
            return placeholder;
        }
    }
}
