package com.shade.platform.model.app;

import com.shade.util.NotNull;

import java.util.ServiceLoader;

public class ApplicationManager {
    private static Application application;

    private ApplicationManager() {
        // prevents instantiation
    }

    @NotNull
    public static Application getApplication() {
        if (application == null) {
            synchronized (ApplicationManager.class) {
                application = ServiceLoader
                    .load(Application.class)
                    .findFirst().orElseThrow(() -> new IllegalStateException("No application found"));
            }
        }

        return application;
    }
}
