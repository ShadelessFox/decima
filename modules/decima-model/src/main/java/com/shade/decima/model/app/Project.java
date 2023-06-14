package com.shade.decima.model.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.Language;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Project implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Project.class);

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Language.class, (JsonDeserializer<?>) (element, type, context) -> Language.values()[element.getAsInt()])
        .create();

    private final ProjectContainer container;
    private final RTTITypeRegistry typeRegistry;
    private final PackfileManager packfileManager;
    private final Compressor compressor;

    Project(@NotNull ProjectContainer container) throws IOException {
        this.container = container;
        this.typeRegistry = new RTTITypeRegistry(container);
        this.compressor = Compressor.acquire(container.getCompressorPath());
        this.packfileManager = new PackfileManager(compressor, getPackfileInfo(container));

        mountDefaults();
    }

    private void mountDefaults() throws IOException {
        packfileManager.mountDefaults(container.getPackfilesPath());
    }

    @NotNull
    public ProjectContainer getContainer() {
        return container;
    }

    @NotNull
    public RTTITypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    @NotNull
    public PackfileManager getPackfileManager() {
        return packfileManager;
    }

    @NotNull
    public Compressor getCompressor() {
        return compressor;
    }

    @NotNull
    public Stream<String> listAllFiles() throws IOException {
        final Path fileListingsPath = container.getFileListingsPath();

        if (fileListingsPath != null) {
            return getListedFiles(fileListingsPath);
        } else {
            return getPrefetchFiles();
        }
    }

    @NotNull
    public Map<Long, long[]> listFileLinks() throws IOException {
        final RTTIObject prefetch = getPrefetchList();

        if (prefetch == null) {
            return Map.of();
        }

        final RTTIObject[] files = prefetch.objs("Files");
        final int[] links = prefetch.get("Links");

        final long[] hashes = new long[files.length];
        final long[][] refs = new long[files.length][];

        for (int i = 0; i < files.length; i++) {
            hashes[i] = PackfileBase.getPathHash(PackfileBase.getNormalizedPath(files[i].str("Path")));
        }

        for (int i = 0, j = 0; i < files.length; i++, j++) {
            final int count = links[j];
            final var current = refs[i] = new long[count];

            for (int k = 0; k < count; k++) {
                current[k] = hashes[links[j + k]];
            }

            j += count;
        }

        final Map<Long, long[]> result = new HashMap<>(refs.length);

        for (int i = 0; i < refs.length; i++) {
            result.put(hashes[i], refs[i]);
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        packfileManager.close();
        compressor.close();
    }

    @NotNull
    private Stream<String> getListedFiles(@NotNull Path path) throws IOException {
        final BufferedReader reader = IOUtils.newCompressedReader(path);

        try {
            return reader.lines().onClose(() -> {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            reader.close();
            throw e;
        }
    }

    @NotNull
    private Stream<String> getPrefetchFiles() throws IOException {
        final RTTIObject list = getPrefetchList();

        if (list == null) {
            return Stream.of();
        }

        final RTTIObject[] files = list.get("Files");

        return Stream.concat(
            Arrays.stream(files).map(entry -> PackfileBase.getNormalizedPath(entry.str("Path"))),
            Arrays.stream(files).map(entry -> PackfileBase.getNormalizedPath(entry.str("Path")) + ".stream")
        );
    }

    @Nullable
    private RTTIObject getPrefetchList() throws IOException {
        final Packfile prefetch = packfileManager.findFirst("prefetch/fullgame.prefetch");

        if (prefetch == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final CoreBinary binary = CoreBinary.from(prefetch.extract("prefetch/fullgame.prefetch"), typeRegistry);

        if (binary.isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        return binary.entries().get(0);
    }

    @Nullable
    private static Map<String, PackfileInfo> getPackfileInfo(@NotNull ProjectContainer container) {
        final Path path = container.getPackfileMetadataPath();

        if (path != null) {
            try (Reader reader = IOUtils.newCompressedReader(container.getPackfileMetadataPath())) {
                return gson.fromJson(reader, new TypeToken<Map<String, PackfileInfo>>() {}.getType());
            } catch (IOException e) {
                log.warn("Can't load packfile name mappings", e);
            }
        }

        return null;
    }
}
