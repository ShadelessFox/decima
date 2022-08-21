package com.shade.platform.model.runtime;

import com.shade.util.NotNull;

public interface ProgressMonitor {
    void begin(@NotNull String title, int ticks);

    void worked(int ticks);

    void done();
}
