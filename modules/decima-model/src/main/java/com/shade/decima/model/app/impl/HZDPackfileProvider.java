package com.shade.decima.model.app.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HZDPackfileProvider implements PackfileProvider {
    @NotNull
    @Override
    public PackfileInfo[] getPackfiles(@NotNull Project project) throws IOException {
        // The game will load all packfiles, but we need to perform some additional work
        // to determine their names and languages used for visuals in the navigator.

        final RTTITypeRegistry registry = project.getTypeRegistry();
        final Set<String> languages = new HashSet<>();

        for (RTTIEnum.Constant lang : registry.<RTTIEnum>find("ELanguage").values()) {
            languages.add(lang.name().toLowerCase(Locale.ROOT));
        }

        final Path root = project.getContainer().getPackfilesPath();
        final List<PackfileInfo> packfiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.bin")) {
            for (Path path : stream) {
                final String name = IOUtils.getBasename(path.getFileName().toString());
                final String[] parts = name.split("_", 2);

                if (parts.length < 2) {
                    packfiles.add(new PackfileInfo(path, parts[0], null));
                } else if (languages.contains(parts[1].toLowerCase(Locale.ROOT))) {
                    packfiles.add(new PackfileInfo(path, parts[0], parts[1]));
                } else {
                    packfiles.add(new PackfileInfo(path, name, null));
                }
            }
        }

        return packfiles.toArray(PackfileInfo[]::new);
    }
}
