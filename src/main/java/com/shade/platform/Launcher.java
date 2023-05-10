package com.shade.platform;

import com.shade.platform.ui.app.Application;
import com.shade.platform.ui.app.ApplicationManager;
import com.shade.platform.ui.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        final Application application = ServiceLoader
            .load(Application.class)
            .findFirst().orElseThrow(() -> new IllegalStateException("No application found"));

        ApplicationManager.setApplication(application);

        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            log.error("Unhandled exception", exception);
            UIUtils.showErrorDialog(application.getFrame(), exception);
        });

        application.start(args);
    }
}
