package com.shade.platform.ui.editors.lazy;

import com.shade.decima.ui.Application;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.icons.LoadingIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.HierarchyEvent;

public class LazyEditor implements Editor {
    private final LazyEditorInput input;
    private final JLabel placeholder;
    private final LoadingIcon icon;

    public LazyEditor(@NotNull LazyEditorInput input) {
        final LoadingIcon icon = new LoadingIcon();

        this.input = input;
        this.placeholder = new JLabel("Initializing\u2026", SwingConstants.CENTER);
        this.placeholder.setIcon(icon);
        this.icon = icon;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
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

        new LoadingWorker(this, input).execute();

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

    private static class LoadingWorker extends SwingWorker<EditorInput, Void> {
        private final LazyEditor editor;
        private final LazyEditorInput input;

        public LoadingWorker(@NotNull LazyEditor editor, @NotNull LazyEditorInput input) {
            this.editor = editor;
            this.input = input;
        }

        @Override
        protected EditorInput doInBackground() throws Exception {
            return input.loadRealInput(new VoidProgressMonitor());
        }

        @Override
        protected void done() {
            final EditorManager manager = Application.getFrame().getEditorManager();

            try {
                manager.reuseEditor(editor, get());
            } catch (Exception e) {
                manager.closeEditor(editor);
                UIUtils.showErrorDialog(e, "Unable to open editor for '%s'".formatted(input.getName()));
            }
        }
    }
}
