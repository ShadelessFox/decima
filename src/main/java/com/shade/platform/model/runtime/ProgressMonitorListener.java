package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public interface ProgressMonitorListener {
    void taskBegin(@NotNull ProgressMonitor.Task task, int ticks);

    void taskEnd(@NotNull ProgressMonitor.Task task);

    void taskWorked(@NotNull ProgressMonitor.Task task, int ticks);
}
