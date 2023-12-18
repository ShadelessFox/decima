package com.shade.decima.model.app.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DSPackfileProvider implements PackfileProvider {
    @NotNull
    @Override
    public PackfileInfo[] getPackfiles(@NotNull Project project) throws IOException {
        // The game will load only a specific set of packfiles, all others are ignored.

        final RTTITypeRegistry registry = project.getTypeRegistry();
        final RTTIEnum packfileCategory = registry.find("EPackFileCategory");
        final RTTIEnum languageCategory = registry.find("EAudioLanguageCategory");
        final Map<String, NameAndLanguage> lookup = new HashMap<>();

        for (int i = 0; i < packfileCategory.values().length - 1; i++) {
            final String name = packfileCategory.valueOf(i).name();

            // Skip the "Patch" packfile, it doesn't have a language.
            if (i < packfileCategory.values().length - 2) {
                for (int j = 0; j < languageCategory.values().length; j++) {
                    final String language = languageCategory.valueOf(j).name();
                    final String hash = getHash("%s_%s".formatted(name, language).toLowerCase(Locale.ROOT));
                    lookup.put(hash, new NameAndLanguage(name, language));
                }
            }

            lookup.put(getHash(name.toLowerCase(Locale.ROOT)).toLowerCase(Locale.ROOT), new NameAndLanguage(name, null));
        }

        final Path root = project.getContainer().getPackfilesPath();
        final List<PackfileInfo> packfiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.bin")) {
            for (Path path : stream) {
                final String name = IOUtils.getBasename(path.getFileName().toString());
                final NameAndLanguage info = lookup.get(name);

                if (info != null) {
                    packfiles.add(new PackfileInfo(path, info.name, info.language));
                }
            }
        }

        return packfiles.toArray(PackfileInfo[]::new);
    }

    @NotNull
    private static String getHash(@NotNull String value) {
        final byte[] src = value.getBytes(StandardCharsets.UTF_8);
        final byte[] dst = new byte[32];
        final long[] hash = MurmurHash3.mmh3(src);

        IOUtils.toHexDigits(hash[0], dst, 0, ByteOrder.LITTLE_ENDIAN);
        IOUtils.toHexDigits(hash[1], dst, 16, ByteOrder.LITTLE_ENDIAN);

        return new String(dst, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
    }

    private record NameAndLanguage(@NotNull String name, @Nullable String language) {}
}
