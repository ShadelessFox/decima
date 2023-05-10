package com.shade.platform.ui.app;

import com.shade.util.NotNull;

import java.util.Objects;

public class ApplicationManager {
    private static Application application;

    private ApplicationManager() {
        // prevents instantiation
    }

    @NotNull
    public static Application getApplication() {
        return Objects.requireNonNull(application, "Application is not set");
    }

    public static void setApplication(@NotNull Application app) {
        if (application != null) {
            throw new IllegalStateException("Application is already set");
        }

        application = app;
    }
}
