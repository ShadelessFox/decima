package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.edit.MemoryChange;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PrefetchUtils {
    private static final Logger log = LoggerFactory.getLogger(PrefetchUtils.class);
    private static final String PREFETCH_PATH = "prefetch/fullgame.prefetch.core";

    private PrefetchUtils() {
        // prevents instantiation
    }

    @Nullable
    public static PrefetchChangeInfo rebuildPrefetch(@NotNull ProgressMonitor monitor, @NotNull Project project, boolean changedFilesOnly) throws IOException {
        final PackfileManager packfileManager = project.getPackfileManager();
        final RTTITypeRegistry typeRegistry = project.getTypeRegistry();

        final Packfile packfile = packfileManager.findFirst(PREFETCH_PATH);

        if (packfile == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final CoreBinary binary = CoreBinary.from(packfile.extract(PREFETCH_PATH), typeRegistry);

        if (binary.isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        final RTTIObject object = binary.entries().get(0);
        final PrefetchList prefetch = PrefetchList.of(object);

        prefetch.rebuild(monitor, packfileManager, typeRegistry, changedFilesOnly);
        prefetch.update(object);

        final byte[] data = binary.serialize(typeRegistry);
        final FilePath path = FilePath.of(PREFETCH_PATH, true);
        final Change change = new MemoryChange(data, path.hash());

        return new PrefetchChangeInfo(packfile, path, change);
    }
}
