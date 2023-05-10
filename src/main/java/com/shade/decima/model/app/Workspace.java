package com.shade.decima.model.app;

import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Workspace implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Workspace.class);

    private final Preferences preferences;

    public Workspace() {
        this.preferences = Preferences.userRoot().node("decima-explorer");
    }

    @NotNull
    public Preferences getPreferences() {
        return preferences;
    }

    @Override
    public void close() {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            log.warn("Error flushing preferences", e);
        }
    }
}
