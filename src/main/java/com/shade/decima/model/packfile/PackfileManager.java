package com.shade.decima.model.packfile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.shade.decima.model.packfile.PackfileBase.getNormalizedPath;
import static com.shade.decima.model.packfile.PackfileBase.getPathHash;

public class PackfileManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileManager.class);

    private final Compressor compressor;
    private final SortedSet<Packfile> packfiles;
    private final Map<String, PackfileInfo> info;

    public PackfileManager(@NotNull Compressor compressor, @Nullable Path packfileInfoPath) {
        Map<String, PackfileInfo> info = null;

        if (packfileInfoPath != null) {
            try (BufferedReader reader = Files.newBufferedReader(packfileInfoPath)) {
                info = new Gson().fromJson(reader, new TypeToken<Map<String, PackfileInfo>>() {}.getType());
            } catch (IOException e) {
                log.warn("Can't load packfile name mappings", e);
            }
        }

        this.compressor = compressor;
        this.packfiles = new TreeSet<>();
        this.info = info;
    }

    public boolean mount(@NotNull Path packfile) throws IOException {
        log.info("Mounting {}", packfile);

        if (!Files.exists(packfile)) {
            log.info("Cannot mount {} because the file does not exist", packfile);
            return false;
        }

        String name = packfile.getFileName().toString();

        if (name.indexOf('.') >= 0) {
            name = name.substring(0, name.indexOf('.'));
        }

        if (info != null && info.containsKey(name)) {
            name = info.get(name).name;
        }

        packfiles.add(new Packfile(
            packfile,
            name,
            FileChannel.open(packfile, StandardOpenOption.READ),
            compressor
        ));

        return true;
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

    private static class PackfileInfo {
        private final String id;
        private final String name;

        public PackfileInfo(@NotNull String id, @NotNull String name) {
            this.id = id;
            this.name = name;
        }
    }
}
