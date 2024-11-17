package com.shade.decima.cli.converters;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ITypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ProjectConverter implements ITypeConverter<Project> {
    private static final Logger log = LoggerFactory.getLogger(ProjectConverter.class);
    private static final List<GamePredicate> predicates = List.of(
        new GamePredicate(
            List.of("ds.exe", "DeathStranding.exe"), // Steam, Epic
            root -> Files.exists(root.resolve("XeFX.dll")) ? GameType.DSDC : GameType.DS,
            root -> root.resolve("data"),
            root -> root.resolve("oo2core_7_win64.dll")
        ),
        new GamePredicate(
            List.of("HorizonZeroDawn.exe"),
            root -> GameType.HZD,
            root -> root.resolve("Packed_DX12"),
            root -> root.resolve("oo2core_3_win64.dll")
        )
    );

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
        ProjectManager manager = ProjectManager.getInstance();

        log.debug("Resolving a project from '{}'", path);

        for (ProjectContainer container : manager.getProjects()) {
            if (Files.isSameFile(container.getExecutablePath().getParent(), path)) {
                log.debug("Found existing project '{}' ({}, {})", container.getName(), container.getId(), container.getType());
                return manager.openProject(container);
            }
        }

        log.debug("No existing project found; creating a temporary one. Detecting game...");
        var result = predicates.stream()
            .flatMap(p -> p.executableNames().stream().map(name -> Map.entry(path.resolve(name), p)))
            .filter(e -> Files.exists(e.getKey()))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Can't determine game type from '" + path + "'"));

        GameType type = result.getValue().typeSupplier().apply(path);
        Path gamePath = result.getKey();
        Path dataPath = result.getValue().dataSupplier().apply(path);
        Path oodlePath = result.getValue().oodleSupplier().apply(path);

        log.debug("Detected project type: {}", type);
        log.debug("Detected data location: {} (readable? {})", dataPath, Files.isReadable(dataPath));
        log.debug("Detected Oodle library: {} (readable? {})", oodlePath, Files.isReadable(oodlePath));

        final ProjectContainer container = new ProjectContainer(
            UUID.randomUUID(),
            "CLI",
            type,
            gamePath,
            dataPath,
            oodlePath
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
        record FromUUID(@NotNull UUID id) implements Source {
        }

        record FromPath(@NotNull Path path) implements Source {
        }
    }

    private record GamePredicate(
        @NotNull List<String> executableNames,
        @NotNull Function<Path, GameType> typeSupplier,
        @NotNull Function<Path, Path> dataSupplier,
        @NotNull Function<Path, Path> oodleSupplier
    ) {
    }
}
