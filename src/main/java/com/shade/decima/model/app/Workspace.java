package com.shade.decima.model.app;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Workspace implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Workspace.class);

    private final Preferences preferences;
    private final Map<String, Project> projects;

    public Workspace() {
        this.preferences = Preferences.userRoot().node("decima-explorer");
        this.projects = new LinkedHashMap<>();

        try {
            loadProjects();
        } catch (BackingStoreException e) {
            log.error("Error reading workspace projects", e);
        }
    }

    @NotNull
    public Collection<Project> getProjects() {
        return projects.values();
    }

    @NotNull
    public Preferences getPreferences() {
        return preferences;
    }

    @Override
    public void close() throws IOException {
        for (Project project : projects.values()) {
            project.close();
        }
    }

    private void loadProjects() throws BackingStoreException {
        final Preferences root = preferences.node("projects");

        for (String id : root.childrenNames()) {
            final Preferences node = root.node(id);
            final String name = node.get("game_name", null);
            final String executablePath = node.get("game_executable_path", null);
            final String archivesPath = node.get("game_archive_root_path", null);
            final String compressorPath = node.get("game_compressor_path", null);
            final String rttiMetaPath = node.get("game_rtti_meta_path", null);
            final String archiveMetaPath = node.get("game_archive_meta_path", null);
            final String gameType = node.get("game_type", null);

            projects.put(id, new Project(
                id,
                name,
                Path.of(executablePath),
                Path.of(archivesPath),
                Path.of(rttiMetaPath),
                archiveMetaPath == null ? null : Path.of(archiveMetaPath),
                Path.of(compressorPath),
                GameType.valueOf(gameType)
            ));
        }
    }
}
