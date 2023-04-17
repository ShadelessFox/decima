package com.shade.platform.ui.editors.lazy;

import com.shade.decima.ui.Application;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.StatefulEditor;
import com.shade.platform.ui.icons.LoadingIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class LazyEditor implements StatefulEditor {
    private final LazyEditorInput input;

    private final LoadingIcon icon;
    private final JLabel label;
    private final JButton button;
    private final LoadingWorker worker;
    private final Timer timer;

    private final Map<String, Object> state = new HashMap<>();
    private boolean focusRequested;

    public LazyEditor(@NotNull LazyEditorInput input) {
        this.input = input;
        this.icon = new LoadingIcon();
        this.label = new JLabel("Editor is not initialized", SwingConstants.CENTER);
        this.button = new JButton("Initialize");
        this.button.addActionListener(e -> Application.getEditorManager().reuseEditor(this, input.canLoadImmediately(true)));

        this.worker = new LoadingWorker();
        this.timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
            label.repaint();
            icon.advance();
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
        focusRequested = true;
        button.requestFocusInWindow();
    }

    @Override
    public boolean isFocused() {
        return focusRequested;
    }

    @Override
    public void loadState(@NotNull Map<String, Object> state) {
        this.state.putAll(state);
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        state.putAll(this.state);
    }

    @Override
    public void dispose() {
        timer.stop();
        worker.cancel(true);
    }

    private void initialize() {
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
        worker.execute();
    }

    private class LoadingWorker extends SwingWorker<EditorInput, Void> {
        @Override
        protected EditorInput doInBackground() throws Exception {
            return input.loadRealInput(new VoidProgressMonitor());
        }

        @Override
        protected void done() {
            if (isCancelled()) {
                return;
            }

            final EditorManager manager = Application.getEditorManager();

            try {
                manager.reuseEditor(LazyEditor.this, get());
            } catch (ExecutionException e) {
                manager.reuseEditor(LazyEditor.this, input.canLoadImmediately(false));
                UIUtils.showErrorDialog(Application.getFrame(), e.getCause(), "Unable to open editor for '%s'".formatted(input.getName()));
            } catch (InterruptedException | CancellationException ignored) {
            }
        }
    }
}
