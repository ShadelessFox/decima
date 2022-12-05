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
import java.awt.*;
import java.awt.event.HierarchyEvent;

public class LazyEditor implements Editor {
    private final LazyEditorInput input;

    private final LoadingIcon icon;
    private final JLabel label;
    private final JButton button;

    public LazyEditor(@NotNull LazyEditorInput input) {
        this.input = input;
        this.icon = new LoadingIcon();
        this.label = new JLabel("Editor is not initialized", SwingConstants.CENTER);
        this.button = new JButton("Initialize");
        this.button.addActionListener(e -> {
            final EditorManager manager = Application.getFrame().getEditorManager();
            for (Editor editor : manager.getEditors()) {
                if (editor.getInput() instanceof LazyEditorInput i && !i.canLoadImmediately()) {
                    manager.reuseEditor(editor, i.canLoadImmediately(true));
                }
            }
        });
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.add(label, BorderLayout.NORTH);
        panel.add(button, BorderLayout.CENTER);

        final JPanel host = new JPanel();
        host.setLayout(new GridBagLayout());
        host.add(panel);

        if (input.canLoadImmediately()) {
            initialize();
        }

        return host;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @Override
    public void setFocus() {
        button.requestFocusInWindow();
    }

    private void initialize() {
        final Timer timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
            label.repaint();
            icon.advance();
        });

        label.setText("Initializing\u2026");
        label.setIcon(icon);
        label.addHierarchyListener(e -> {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (label.isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });

        button.setVisible(false);

        new LoadingWorker(this, input).execute();
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
                manager.reuseEditor(editor, input.canLoadImmediately(false));
                UIUtils.showErrorDialog(e, "Unable to open editor for '%s'".formatted(input.getName()));
            }
        }
    }
}
