package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public interface ProgressMonitor {
    @NotNull
    Task begin(@NotNull String title, int total);

    interface Task extends AutoCloseable {
        @NotNull
        ProgressMonitor split(int ticks);

        @NotNull
        String title();

        void worked(int ticks);

        @Override
        void close();
    }
}
