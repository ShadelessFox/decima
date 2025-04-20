package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public class VoidProgressMonitor implements ProgressMonitor {
    @NotNull
    @Override
    public IndeterminateTask begin(@NotNull String title) {
        return new VoidTask();
    }

    @NotNull
    @Override
    public Task begin(@NotNull String title, int total) {
        return new VoidTask();
    }

    private static class VoidTask implements Task {
        @NotNull
        @Override
        public ProgressMonitor split(int ticks) {
            return new VoidProgressMonitor();
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
            return false;
        }

        @NotNull
        @Override
        public String title() {
            return "void";
        }

    }
}
