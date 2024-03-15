package com.shade.decima.cli.converters;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ITypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

public class ProjectConverter implements ITypeConverter<Project> {
    private static final Logger log = LoggerFactory.getLogger(ProjectConverter.class);

    @Override
    public Project convert(String value) throws Exception {
        final Source source = determineSource(value);

        if (source instanceof Source.FromUUID s) {
            return openProject(s.id());
        } else if (source instanceof Source.FromPath s) {
            return createTempProject(s.path());
        } else {
            throw new NotImplementedException();
        }
    }

    @NotNull
    private static Project createTempProject(@NotNull Path path) throws IOException {
        final ProjectManager manager = ProjectManager.getInstance();
        final GameType type;
        final Path oodlePath;
        final Path gamePath;
        final Path dataPath;

        log.debug("Resolving a project from '{}'", path);

        for (ProjectContainer container : manager.getProjects()) {
            if (Files.isSameFile(container.getExecutablePath().getParent(), path)) {
                log.debug("Found existing project '{}' ({})", container.getName(), container.getId());
                return manager.openProject(container);
            }
        }

        log.debug("No existing project found; creating a temporary one");
        try (Stream<Path> stream = Files.list(path)) {
            oodlePath = stream
                .filter(p -> IOUtils.getBasename(p).startsWith("oo2core"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find oodle library in '" + path + "'"));
        }

        if (Files.exists(path.resolve("ds.exe"))) {
            // It's not good to rely on the presence of XeFX because it wasn't there on the initial release
            type = Files.exists(path.resolve("xefx.dll")) ? GameType.DSDC : GameType.DS;
            gamePath = path.resolve("ds.exe");
            dataPath = path.resolve("data");
        } else if (Files.exists(path.resolve("horizonzerodawn.exe"))) {
            type = GameType.HZD;
            gamePath = path.resolve("horizonzerodawn.exe");
            dataPath = path.resolve("Packed_DX12");
        } else {
            throw new IllegalArgumentException("Can't determine game type from '" + path + "'");
        }

        log.debug("Detected project type: {}", type);
        log.debug("Detected data location: {}", dataPath);
        log.debug("Detected oodle library: {}", oodlePath);

        final ProjectContainer container = new ProjectContainer(
            UUID.randomUUID(),
            "CLI",
            type,
            gamePath,
            dataPath,
            oodlePath,
            type.getKnownRttiTypesPath(),
            type.getKnownFileListingsPath()
        );

        manager.addProject(container);

        return manager.openProject(container);
    }

    @NotNull
    private static Project openProject(@NotNull UUID id) throws IOException {
        final ProjectManager manager = ProjectManager.getInstance();
        final ProjectContainer container = manager.getProject(id);

        if (container == null) {
            throw new IllegalArgumentException("Can't find project '" + id + "'");
        }

        log.debug("Found project '{}' ({})", container.getName(), container.getId());

        return manager.openProject(container);
    }

    @NotNull
    private static Source determineSource(@NotNull String value) {
        try {
            return new Source.FromUUID(UUID.fromString(value));
        } catch (Exception ignored) {
        }

        try {
            return new Source.FromPath(Path.of(value));
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Can't determine project source from '" + value + "'");
    }

    private sealed interface Source {
        record FromUUID(@NotNull UUID id) implements Source {}

        record FromPath(@NotNull Path path) implements Source {}
    }
}
