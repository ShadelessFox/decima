package com.shade.platform.ui.dialogs;

import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProgressDialog extends BaseDialog {
    private static final DataKey<ProgressMonitor.IndeterminateTask> TASK_KEY = new DataKey<>("task", ProgressMonitor.IndeterminateTask.class);
    private static final int INDETERMINATE = -1;

    private final JPanel taskPanel;
    private final SwingWorker<Object, Exception> executor;
    private final Deque<TaskEvent> events = new ArrayDeque<>();
    private final Timer timer;
    private final Taskbar taskbar;

    private ProgressDialog(@NotNull String title, @NotNull Worker<?, ?> worker) {
        super(title);
        this.taskPanel = new JPanel();
        this.taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.PAGE_AXIS));

        if (Taskbar.isTaskbarSupported()) {
            taskbar = Taskbar.getTaskbar();
        } else {
            taskbar = null;
        }

        this.executor = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    return worker.doInBackground(new MyProgressMonitor(this, taskbar));
                } finally {
                    SwingUtilities.invokeLater(ProgressDialog.this::close);
                }
            }
        };

        this.timer = new Timer(100, e -> {
            synchronized (taskPanel.getTreeLock()) {
                while (!events.isEmpty()) {
                    try {
                        events.remove().update(this);
                    } catch (Throwable ex) {
                        UIUtils.showErrorDialog(ex);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T, E extends Exception> Optional<T> showProgressDialog(@Nullable Window owner, @NotNull String title, @NotNull Worker<T, E> worker) throws E {
        final ProgressDialog dialog = new ProgressDialog(title, worker);
        final SwingWorker<Object, Exception> executor = dialog.executor;

        dialog.showDialog(owner);

        if (dialog.taskbar != null) {
            dialog.taskbar.setWindowProgressState(JOptionPane.getRootFrame(), Taskbar.State.OFF);
            dialog.taskbar.setWindowProgressValue(JOptionPane.getRootFrame(), 0);
        }

        try {
            return Optional.ofNullable((T) executor.get());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw (E) e.getCause();
        } catch (CancellationException | InterruptedException e) {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JScrollPane pane = new JScrollPane(taskPanel);
        pane.setPreferredSize(new Dimension(420, 200));
        pane.addHierarchyListener(e -> {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (pane.isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });

        return pane;
    }

    @NotNull
    @Override
    protected JDialog createDialog(@Nullable Window owner) {
        final JDialog dialog = super.createDialog(owner);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                executor.execute();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (executor.isDone()) {
                    dialog.dispose();
                }
            }
        });

        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        return dialog;
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_CANCEL};
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_CANCEL;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        getButton(descriptor).setEnabled(false);
        executor.cancel(false);
    }

    @NotNull
    private TaskComponent findTaskComponent(@NotNull ProgressMonitor.IndeterminateTask task) {
        for (int i = 0; i < taskPanel.getComponentCount(); i++) {
            final TaskComponent component = (TaskComponent) taskPanel.getComponent(i);
            final ProgressMonitor.IndeterminateTask other = TASK_KEY.get(component);

            if (other == task) {
                return component;
            }
        }

        throw new IllegalArgumentException("Can't find component for the given task");
    }

    private void submitEvent(@NotNull TaskEvent event) {
        events.offer(event);
    }

    public interface Worker<T, E extends Exception> {
        T doInBackground(@NotNull ProgressMonitor monitor) throws E;
    }

    private static class TaskComponent extends JComponent {
        private final ProgressMonitor.IndeterminateTask task;
        private final int total;

        private final JLabel label;
        private final JProgressBar progressBar;

        private int worked = 0;

        public TaskComponent(@NotNull ProgressMonitor.IndeterminateTask task, int total) {
            this.task = task;
            this.total = total;

            setLayout(new MigLayout("ins panel", "[grow,fill]", "[][]"));
            add(label = new JLabel(), "wrap");
            add(progressBar = new JProgressBar());

            if (total != INDETERMINATE) {
                label.setText("%s (0/%d)".formatted(task.title(), total));
                progressBar.setMaximum(total);
            } else {
                label.setText(task.title());
                progressBar.setIndeterminate(true);
            }

            putClientProperty(TASK_KEY, task);
        }

        @Override
        public Dimension getMaximumSize() {
            final Dimension size = getPreferredSize();
            return new Dimension(Short.MAX_VALUE, size.height);
        }

        public void worked(int ticks) {
            if (worked + ticks > total) {
                throw new IllegalArgumentException("Too many work to do while performing task '" + task.title() + "'");
            }

            worked += ticks;

            progressBar.setValue(worked);
            label.setText("%s (%d/%d)".formatted(task.title(), worked, total));
        }
    }

    private class MyProgressMonitor implements ProgressMonitor {
        protected final Future<?> future;
        protected final Taskbar taskbar;

        public MyProgressMonitor(@NotNull Future<?> future, @Nullable Taskbar taskbar) {
            this.future = future;
            this.taskbar = taskbar;
        }

        @NotNull
        @Override
        public IndeterminateTask begin(@NotNull String title) {
            return new MyProgressMonitorTask<>(this, title, INDETERMINATE);
        }

        @NotNull
        @Override
        public Task begin(@NotNull String title, int total) {
            return new MyProgressMonitorTask<>(this, title, total);
        }
    }

    private class MySubProgressMonitor extends MyProgressMonitor {
        private final MyProgressMonitorTask<?> task;
        private final int provided;

        public MySubProgressMonitor(@NotNull MyProgressMonitorTask<?> task, int provided) {
            super(task.monitor.future, null);
            this.task = task;
            this.provided = provided;
        }

        @NotNull
        @Override
        public IndeterminateTask begin(@NotNull String title) {
            return new MySubProgressMonitorTask(this, title, INDETERMINATE);
        }

        @NotNull
        @Override
        public Task begin(@NotNull String title, int total) {
            return new MySubProgressMonitorTask(this, title, total);
        }
    }

    private class MyProgressMonitorTask<T extends MyProgressMonitor> implements ProgressMonitor.Task {
        protected final T monitor;
        private final String title;

        private MyProgressMonitorTask(@NotNull T monitor, @NotNull String title, int total) {
            this.monitor = monitor;
            this.title = title;

            submitEvent(new TaskEvent.Begin(this, total));
        }

        @NotNull
        @Override
        public ProgressMonitor split(int ticks) {
            return new MySubProgressMonitor(this, ticks);
        }

        @Override
        public void worked(int ticks) {
            submitEvent(new TaskEvent.Worked(this, ticks));
        }

        @Override
        public void close() {
            submitEvent(new TaskEvent.End(this));
        }

        @Override
        public boolean isCanceled() {
            return monitor.future.isCancelled();
        }

        @NotNull
        @Override
        public String title() {
            return title;
        }
    }

    private class MySubProgressMonitorTask extends MyProgressMonitorTask<MySubProgressMonitor> {
        private MySubProgressMonitorTask(@NotNull MySubProgressMonitor monitor, @NotNull String title, int total) {
            super(monitor, title, total);
        }

        @Override
        public void close() {
            monitor.task.worked(monitor.provided);
            super.close();
        }
    }

    private sealed interface TaskEvent {
        void update(@NotNull ProgressDialog dialog);

        record Begin(@NotNull MyProgressMonitorTask<?> task, int ticks) implements TaskEvent {
            @Override
            public void update(@NotNull ProgressDialog dialog) {
                dialog.taskPanel.add(new TaskComponent(task, ticks));
                dialog.taskPanel.revalidate();
                dialog.taskPanel.repaint();

                final Taskbar taskbar = task.monitor.taskbar;
                if (taskbar != null) {
                    taskbar.setWindowProgressState(
                        JOptionPane.getRootFrame(),
                        ticks == INDETERMINATE ? Taskbar.State.INDETERMINATE : Taskbar.State.NORMAL
                    );
                }

                // Do this at the end so it won't break any following events
                if (task.monitor instanceof MySubProgressMonitor sub) {
                    final TaskComponent component = dialog.findTaskComponent(sub.task);
                    if (component.total <= component.worked) {
                        throw new IllegalStateException("Can't begin a new task '%s' because no more ticks are left in parent task '%s' (allocated: %d, left: %d)".formatted(
                            task.title,
                            sub.task.title,
                            component.total,
                            component.total - component.worked
                        ));
                    }
                }
            }
        }

        record End(@NotNull MyProgressMonitorTask<?> task) implements TaskEvent {
            @Override
            public void update(@NotNull ProgressDialog dialog) {
                dialog.taskPanel.remove(dialog.findTaskComponent(task));
                dialog.taskPanel.revalidate();
                dialog.taskPanel.repaint();

                final Taskbar taskbar = task.monitor.taskbar;
                if (taskbar != null) {
                    taskbar.setWindowProgressState(
                        JOptionPane.getRootFrame(),
                        Taskbar.State.OFF
                    );
                }
            }
        }

        record Worked(@NotNull MyProgressMonitorTask<?> task, int ticks) implements TaskEvent {
            @Override
            public void update(@NotNull ProgressDialog dialog) {
                final TaskComponent component = dialog.findTaskComponent(task);
                component.worked(ticks);

                final Taskbar taskbar = task.monitor.taskbar;
                if (taskbar != null) {
                    taskbar.setWindowProgressValue(
                        JOptionPane.getRootFrame(),
                        component.worked * 100 / component.total
                    );
                }
            }
        }
    }
}
