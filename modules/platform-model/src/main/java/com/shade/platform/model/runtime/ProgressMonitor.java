package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public interface ProgressMonitor {
    @NotNull
    IndeterminateTask begin(@NotNull String title);

    @NotNull
    Task begin(@NotNull String title, int total);

    interface IndeterminateTask extends AutoCloseable {
        @NotNull
        String title();

        @Override
        void close();
    }

    interface Task extends IndeterminateTask {
        @NotNull
        ProgressMonitor split(int ticks);

        void worked(int ticks);
    }
}
