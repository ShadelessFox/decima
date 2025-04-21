package com.shade.decima.model.app;

import com.shade.decima.model.app.impl.DSPackfileProvider;
import com.shade.decima.model.app.impl.HZDPackfileProvider;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.packfile.oodle.Oodle;
import com.shade.decima.model.packfile.prefetch.PrefetchUpdater;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader;
import com.shade.decima.model.rtti.RTTICoreFileReader.ThrowingErrorHandlingStrategy;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Project implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Project.class);

    private final ProjectContainer container;
    private final RTTITypeRegistry typeRegistry;
    private final RTTICoreFileReader coreFileReader;
    private final PackfileManager packfileManager;
    private final Oodle compressor;

    Project(@NotNull ProjectContainer container) throws IOException {
        this.container = container;
        this.typeRegistry = new RTTITypeRegistry(container);
        this.coreFileReader = new CoreBinary.Reader(typeRegistry);
        this.compressor = Oodle.acquire(container.getCompressorPath());
        this.packfileManager = new PackfileManager(compressor);

        mountDefaults();
    }

    // TODO: Should be specific to the archive manager, hence should be moved to the concrete implementation
    private void mountDefaults() throws IOException {
        final PackfileProvider packfileProvider = switch (container.getType()) {
            case DS, DSDC -> new DSPackfileProvider();
            case HZD -> new HZDPackfileProvider();
        };

        final long start = System.currentTimeMillis();

        Arrays.stream(packfileProvider.getPackfiles(this)).parallel().forEach(info -> {
            try {
                packfileManager.mountPackfile(info);
            } catch (IOException e) {
                log.error("Can't mount packfile '{}'", info.path(), e);
            }
        });

        log.info("Found and mounted {} packfiles in {} ms", packfileManager.getArchives().size(), System.currentTimeMillis() - start);
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
    public RTTICoreFileReader getCoreFileReader() {
        return coreFileReader;
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
        final BufferedReader reader = container.getFilePaths();

        return reader
            .lines()
            .onClose(IOUtils.asUncheckedRunnable(reader));
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
            hashes[i] = Packfile.getPathHash(Packfile.getNormalizedPath(files[i].str("Path")));
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

    // TODO: Replace with com.shade.decima.model.packfile.prefetch.PrefetchList
    @Nullable
    private RTTIObject getPrefetchList() throws IOException {
        final Packfile prefetch = packfileManager.findFirst(PrefetchUpdater.PREFETCH_PATH);

        if (prefetch == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final RTTICoreFile file = coreFileReader.read(prefetch.getFile(PrefetchUpdater.PREFETCH_PATH), ThrowingErrorHandlingStrategy.getInstance());

        if (file.objects().isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        return file.objects().get(0);
    }
}
