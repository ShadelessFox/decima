package com.shade.decima.model.packfile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.model.rtti.objects.Language;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

import static com.shade.decima.model.packfile.PackfileBase.*;

public class PackfileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private static final String PACKFILE_EXTENSION = ".bin";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Language.class, (JsonDeserializer<Object>) (json, type, context) -> Language.values()[json.getAsInt()])
        .create();

    private final Compressor compressor;
    private final SortedSet<Packfile> packfiles;
    private final Map<String, PackfileInfo> packfilesInfo;

    public PackfileManager(@NotNull Compressor compressor, @Nullable Path packfileInfoPath) {
        Map<String, PackfileInfo> info = null;

        if (packfileInfoPath != null) {
            try (Reader reader = IOUtils.newCompressedReader(packfileInfoPath)) {
                info = GSON.fromJson(reader, new TypeToken<Map<String, PackfileInfo>>() {}.getType());
            } catch (IOException e) {
                log.warn("Can't load packfile name mappings", e);
            }
        }

        this.compressor = compressor;
        this.packfiles = new TreeSet<>();
        this.packfilesInfo = info;
    }

    public void mount(@NotNull Path packfile) throws IOException {
        if (Files.notExists(packfile)) {
            return;
        }

        String name = packfile.getFileName().toString();

        if (name.indexOf('.') >= 0) {
            name = name.substring(0, name.indexOf('.'));
        }

        final PackfileInfo info = packfilesInfo != null
            ? packfilesInfo.get(name)
            : null;

        packfiles.add(new Packfile(
            FileChannel.open(packfile, StandardOpenOption.READ),
            compressor,
            info,
            packfile
        ));

        log.info("Mounted '{}'", packfile);
    }

    public void mountDefaults(@NotNull Path root) throws IOException {
        try (Stream<Path> stream = listPackfiles(root).parallel()) {
            stream.filter(PackfileManager::isValidPackfile).forEach(path -> {
                try {
                    mount(path);
                } catch (IOException e) {
                    log.error("Unable to mount packfile '" + path + "'", e);
                }
            });
        }
    }

    @Nullable
    public Packfile findAny(@NotNull String path) {
        return findAny(getPathHash(getNormalizedPath(path)));
    }

    @Nullable
    public Packfile findAny(long hash) {
        return packfiles.stream()
            .filter(x -> x.getFileEntry(hash) != null)
            .findAny().orElse(null);
    }


    @NotNull
    public List<Packfile> findAll(@NotNull String path) {
        return findAll(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public List<Packfile> findAll(long hash) {
        return packfiles.stream()
            .filter(x -> x.getFileEntry(hash) != null)
            .toList();
    }

    @NotNull
    public Collection<Packfile> getPackfiles() {
        return packfiles;
    }

    @Override
    public void close() throws IOException {
        for (Packfile packfile : packfiles) {
            packfile.close();
        }

        packfiles.clear();
    }


    @NotNull
    private Stream<Path> listPackfiles(@NotNull Path root) throws IOException {
        if (packfilesInfo != null) {
            return packfilesInfo
                .keySet().stream()
                .map(name -> root.resolve(name + PACKFILE_EXTENSION));
        } else {
            return Files.list(root);
        }
    }

    private static boolean isValidPackfile(@NotNull Path path) {
        return path.getFileName().toString().endsWith(PACKFILE_EXTENSION);
    }
}
