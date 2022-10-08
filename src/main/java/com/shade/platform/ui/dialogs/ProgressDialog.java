package com.shade.platform.ui.dialogs;

import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;

public class ProgressDialog extends BaseDialog {
    private static final DataKey<ProgressMonitor.Task> TASK_KEY = new DataKey<>("task", ProgressMonitor.Task.class);

    private final Worker<?, ?> worker;
    private final JPanel taskPanel;
    private final ProgressMonitorListener listener;

    private volatile Object result;
    private volatile Exception exception;

    private ProgressDialog(@NotNull String title, @NotNull Worker<?, ?> worker) {
        super(title, List.of(BUTTON_CANCEL));
        this.worker = worker;
        this.taskPanel = new JPanel();
        this.taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.PAGE_AXIS));

        this.listener = new ProgressMonitorListener() {
            @Override
            public void taskBegin(@NotNull ProgressMonitor.Task task, int ticks) {
                taskPanel.add(new TaskComponent(task, ticks));
                taskPanel.revalidate();
                taskPanel.repaint();
            }

            @Override
            public void taskEnd(@NotNull ProgressMonitor.Task task) {
                taskPanel.remove(findTaskComponent(task));
                taskPanel.revalidate();
                taskPanel.repaint();
            }

            @Override
            public void taskWorked(@NotNull ProgressMonitor.Task task, int ticks) {
                findTaskComponent(task).worked(ticks);
            }

            @NotNull
            private TaskComponent findTaskComponent(@NotNull ProgressMonitor.Task task) {
                final int count = taskPanel.getComponentCount();

                for (int i = 0; i < count; i++) {
                    final TaskComponent component = (TaskComponent) taskPanel.getComponent(i);
                    final ProgressMonitor.Task other = TASK_KEY.get(component);

                    if (other == task) {
                        return component;
                    }
                }

                throw new IllegalArgumentException("Can't find component for the given task");
            }
        };
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T, E extends Exception> Optional<T> showProgressDialog(@Nullable Window owner, @NotNull String title, @NotNull Worker<T, E> worker) throws E {
        final ProgressDialog dialog = new ProgressDialog(title, worker);
        final ButtonDescriptor result = dialog.showDialog(owner);

        if (result == BUTTON_CANCEL) {
            return Optional.empty();
        } else if (dialog.exception != null) {
            throw (E) dialog.exception;
        } else {
            return Optional.ofNullable((T) dialog.result);
        }
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JScrollPane pane = new JScrollPane(taskPanel);
        pane.setPreferredSize(new Dimension(420, 200));
        return pane;
    }

    @NotNull
    @Override
    protected JDialog createDialog(@Nullable Window owner) {
        final JDialog dialog = super.createDialog(owner);
        final SwingWorker<Object, Object> executor = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                return worker.doInBackground(new MyProgressMonitor(listener));
            }

            @Override
            protected void done() {
                try {
                    result = get();
                } catch (CancellationException ignored) {
                    return;
                } catch (Exception e) {
                    exception = e;
                }

                buttonPressed(BUTTON_OK);
            }
        };

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                executor.execute();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                executor.cancel(true);
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_CANCEL;
    }

    public interface Worker<T, E extends Exception> {
        T doInBackground(@NotNull ProgressMonitor monitor) throws E;
    }

    private static class TaskComponent extends JComponent {
        private final ProgressMonitor.Task task;
        private final int total;

        private final JLabel label;
        private final JProgressBar progressBar;

        private int worked = 0;

        public TaskComponent(@NotNull ProgressMonitor.Task task, int total) {
            this.task = task;
            this.total = total;

            setLayout(new MigLayout("ins panel", "[grow,fill]", "[][]"));
            add(label = new JLabel("%s (0/%d)".formatted(task.title(), total)), "wrap");
            add(progressBar = new JProgressBar(0, total));

            putClientProperty(TASK_KEY, task);
        }

        @Override
        public Dimension getMaximumSize() {
            final Dimension size = getPreferredSize();
            return new Dimension(Short.MAX_VALUE, size.height);
        }

        public void worked(int ticks) {
            if (worked + ticks > total) {
                throw new IllegalArgumentException("Too many work to do");
            }

            worked += ticks;

            progressBar.setValue(worked);
            label.setText("%s (%d/%d)".formatted(task.title(), worked, total));
        }
    }

    private static class MyProgressMonitor implements ProgressMonitor {
        protected final ProgressMonitorListener listener;

        public MyProgressMonitor(@NotNull ProgressMonitorListener listener) {
            this.listener = listener;
        }

        @NotNull
        @Override
        public Task begin(@NotNull String title, int total) {
            return new MyProgressMonitorTask<>(this, title, total);
        }
    }

    private static class MySubProgressMonitor extends MyProgressMonitor {
        private final MyProgressMonitorTask<?> task;
        private final int provided;

        public MySubProgressMonitor(@NotNull ProgressMonitorListener listener, @NotNull MyProgressMonitorTask<?> task, int provided) {
            super(listener);
            this.task = task;
            this.provided = provided;
        }

        @NotNull
        @Override
        public Task begin(@NotNull String title, int total) {
            return new MySubProgressMonitorTask(this, title, total);
        }
    }

    private static class MyProgressMonitorTask<T extends MyProgressMonitor> implements ProgressMonitor.Task {
        protected final T monitor;
        private final String title;

        private MyProgressMonitorTask(@NotNull T monitor, @NotNull String title, int total) {
            this.monitor = monitor;
            this.title = title;
            this.monitor.listener.taskBegin(this, total);
        }

        @NotNull
        @Override
        public ProgressMonitor split(int ticks) {
            return new MySubProgressMonitor(monitor.listener, this, ticks);
        }

        @Override
        public void worked(int ticks) {
            monitor.listener.taskWorked(this, ticks);
        }

        @Override
        public void close() {
            monitor.listener.taskEnd(this);
        }

        @NotNull
        @Override
        public String title() {
            return title;
        }
    }

    private static class MySubProgressMonitorTask extends MyProgressMonitorTask<MySubProgressMonitor> {
        private MySubProgressMonitorTask(@NotNull MySubProgressMonitor monitor, @NotNull String title, int total) {
            super(monitor, title, total);
        }

        @Override
        public void close() {
            monitor.task.worked(monitor.provided);
            super.close();
        }
    }

    private interface ProgressMonitorListener {
        void taskBegin(@NotNull ProgressMonitor.Task task, int ticks);

        void taskEnd(@NotNull ProgressMonitor.Task task);

        void taskWorked(@NotNull ProgressMonitor.Task task, int ticks);
    }
}
