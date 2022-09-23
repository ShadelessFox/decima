package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class NavigatorPackfileNode extends NavigatorFolderNode {
    private static final Logger log = LoggerFactory.getLogger(NavigatorPackfileNode.class);

    private final Project project;
    private final Packfile packfile;
    private final TreeSet<FilePath> files;

    public NavigatorPackfileNode(@NotNull NavigatorProjectNode parent, @NotNull Packfile packfile) {
        super(parent, FilePath.EMPTY_PATH);
        this.project = parent.getProject();
        this.packfile = packfile;
        this.files = new TreeSet<>();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final Set<Long> containing = new HashSet<>();
        final RTTIObject prefetch = getPrefetch();

        if (prefetch != null) {
            for (RTTIObject file : prefetch.<RTTIObject[]>get("Files")) {
                final String path = PackfileBase.getNormalizedPath(file.get("Path"));
                final long hash = PackfileBase.getPathHash(path);

                if (packfile.contains(hash)) {
                    files.add(new FilePath(path.split("/"), hash));
                    containing.add(hash);
                }
            }
        }

        for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
            if (!containing.contains(entry.hash())) {
                files.add(new FilePath(new String[]{"<unnamed>", Long.toHexString(entry.hash())}, entry.hash()));
            }
        }

        return super.loadChildren(monitor);
    }

    @NotNull
    @Override
    public String getLabel() {
        final PackfileInfo info = packfile.getInfo();
        if (info != null && info.getLang() != null) {
            return packfile.getName() + " (" + info.getLang().getLabel() + ")";
        } else {
            return packfile.getName();
        }
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Navigator.archiveIcon");
    }

    @NotNull
    @Override
    public Packfile getPackfile() {
        return packfile;
    }

    @NotNull
    public SortedSet<FilePath> getFiles(@NotNull FilePath path) {
        return files.subSet(path, path.concat("*"));
    }

    @NotNull
    @Override
    protected SortedSet<FilePath> getFilesForPath() {
        return files;
    }

    @Nullable
    private RTTIObject getPrefetch() throws IOException {
        final Packfile prefetch = project.getPackfileManager().findAny("prefetch/fullgame.prefetch");

        if (prefetch == null) {
            log.error("Can't find prefetch file");
            return null;
        }

        final CoreBinary binary = CoreBinary.from(prefetch.extract("prefetch/fullgame.prefetch"), project.getTypeRegistry());

        if (binary.isEmpty()) {
            log.error("Prefetch file is empty");
            return null;
        }

        return binary.entries().get(0);
    }
}
