package com.shade.decima.ui.data;

import com.shade.platform.model.Disposable;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.icons.LoadingIcon;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.concurrent.Future;

/**
 * Represents a panel that displays a value viewer with a support for asynchronous reload.
 */
public class ValueViewerPanel extends JComponent implements Disposable {
    private static final int DELAY_BEFORE_SHOWING_LOADER_MS = 100;
    private static final String PAGE_LOADING = "loading";
    private static final String PAGE_VIEWER = "viewer";

    public interface Callback {
        void viewerChanged(@NotNull ValueViewerPanel panel);

        void viewerClosed(@NotNull ValueViewerPanel panel);
    }

    private ValueViewer currentViewer;
    private JComponent currentComponent;
    private SwingWorker<?, ?> currentWorker;

    public ValueViewerPanel() {
        setLayout(new CardLayout());
        setPreferredSize(new Dimension(400, 400));
    }

    /**
     * Updates the current viewer, optionally swapping it with a supplied {@code viewer}, with the given {@code controller}.
     *
     * @param viewer     the viewer to use
     * @param controller the value controller to supply to the viewer
     * @param callback   the callback to notify about the viewer changes
     */
    public void update(@NotNull ValueViewer viewer, @NotNull ValueController<?> controller, @NotNull Callback callback) {
        final boolean viewerChanged = currentViewer != viewer;

        if (viewerChanged) {
            if (currentComponent instanceof Disposable d) {
                d.dispose();
            }

            currentViewer = viewer;
            currentComponent = viewer.createComponent();

            add(currentComponent, PAGE_VIEWER);
            callback.viewerChanged(this);
        }

        final CardLayout layout = (CardLayout) getLayout();
        layout.show(this, PAGE_VIEWER);

        final SwingWorker<?, ?> worker = currentWorker;
        if (worker != null) {
            worker.cancel(false);
        }

        currentWorker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() {
                currentViewer.refresh(new MyProgressMonitor(this), currentComponent, controller);
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    callback.viewerClosed(ValueViewerPanel.this);
                } else {
                    layout.show(ValueViewerPanel.this, PAGE_VIEWER);
                }

                currentWorker = null;
            }
        };
        currentWorker.execute();

        final long start = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (currentWorker == null || currentWorker.isDone() || currentWorker.isCancelled()) {
                    return;
                }
                if (System.currentTimeMillis() - start > DELAY_BEFORE_SHOWING_LOADER_MS) {
                    final JPanel inner = new JPanel();
                    inner.setLayout(new GridBagLayout());
                    inner.add(new LoadingPane(currentWorker));

                    add(inner, PAGE_LOADING);
                    layout.show(ValueViewerPanel.this, PAGE_LOADING);
                } else {
                    SwingUtilities.invokeLater(this);
                }
            }
        });
    }

    @Override
    public void dispose() {
        if (currentComponent instanceof Disposable d) {
            d.dispose();
        }

        final SwingWorker<?, ?> worker = currentWorker;
        if (worker != null) {
            worker.cancel(false);
        }

        currentViewer = null;
        currentComponent = null;
        currentWorker = null;

        removeAll();
    }

    private static class LoadingPane extends JComponent {
        public LoadingPane(@NotNull Future<?> future) {
            final LoadingIcon icon = new LoadingIcon();
            final JLabel label = new JLabel("Loading\u2026", icon, SwingConstants.LEADING);

            final Timer timer = new Timer(1000 / LoadingIcon.SEGMENTS, e -> {
                label.repaint();
                icon.advance();
            });

            label.addHierarchyListener(e -> {
                if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (label.isShowing()) {
                        timer.start();
                    } else {
                        timer.stop();
                    }
                }
            });

            final JButton button = new JButton("Cancel");
            button.addActionListener(e -> {
                button.setEnabled(false);
                future.cancel(false);
            });

            setLayout(new MigLayout("ins panel,wrap", "[center]"));
            add(label);
            add(button);
        }
    }

    private static class MyProgressMonitor implements ProgressMonitor {
        private final Future<?> future;

        public MyProgressMonitor(@NotNull Future<?> future) {
            this.future = future;
        }

        @NotNull
        @Override
        public IndeterminateTask begin(@NotNull String title) {
            return new MyTask(this);
        }

        @NotNull
        @Override
        public Task begin(@NotNull String title, int total) {
            return new MyTask(this);
        }

        private static class MyTask implements Task {
            private final MyProgressMonitor monitor;

            public MyTask(@NotNull MyProgressMonitor monitor) {
                this.monitor = monitor;
            }

            @NotNull
            @Override
            public ProgressMonitor split(int ticks) {
                return new MyProgressMonitor(monitor.future);
            }

            @Override
            public void worked(int ticks) {
                // do nothing
            }

            @Override
            public void close() {
                // do nothing
            }

            @Override
            public boolean isCanceled() {
                return monitor.future.isCancelled();
            }

            @NotNull
            @Override
            public String title() {
                return "";
            }
        }
    }
}
