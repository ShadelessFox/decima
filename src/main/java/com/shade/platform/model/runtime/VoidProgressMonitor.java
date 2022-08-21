package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public class VoidProgressMonitor implements ProgressMonitor {
    @Override
    public void begin(@NotNull String title, int ticks) {
        // do nothing
    }

    @Override
    public void worked(int ticks) {
        // do nothing
    }

    @Override
    public void done() {
        // do nothing
    }
}
