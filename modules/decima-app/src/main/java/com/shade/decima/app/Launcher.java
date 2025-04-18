package com.shade.decima.app;

import com.shade.platform.model.app.Application;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.ui.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        final Application application = ApplicationManager.getApplication();

        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            log.error("Unhandled exception", exception);
            UIUtils.showErrorDialog(exception);
        });

        application.start(args);
    }
}
