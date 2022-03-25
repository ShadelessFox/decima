package com.shade.decima.model.app.runtime;

import com.shade.decima.model.util.NotNull;

public interface ProgressMonitor {
    void begin(@NotNull String title, int ticks);

    void worked(int ticks);

    void done();
}
