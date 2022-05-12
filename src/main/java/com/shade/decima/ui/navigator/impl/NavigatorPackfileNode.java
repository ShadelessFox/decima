package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.base.CoreObject;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.navigator.NavigatorNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final PackfileManager manager = project.getPackfileManager();
        final Packfile prefetch = manager.findAny("prefetch/fullgame.prefetch");

        if (prefetch == null) {
            log.error("Can't find prefetch file");
            return EMPTY_CHILDREN;
        }

        final CoreObject root = CoreObject.from(prefetch.extract("prefetch/fullgame.prefetch"), project.getTypeRegistry());

        if (root.isEmpty()) {
            log.error("Prefetch file is empty");
            return EMPTY_CHILDREN;
        }

        final RTTIObject object = root.getEntries().get(0);
        final Set<Long> containing = new HashSet<>();

        for (RTTIObject file : object.<RTTICollection<RTTIObject>>get("Files")) {
            final String path = PackfileBase.getNormalizedPath(file.get("Path"));
            final long hash = PackfileBase.getPathHash(path);

            if (packfile.contains(hash)) {
                files.add(new FilePath(path.split("/"), hash));
                containing.add(hash);
            }
        }

        for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
            if (!containing.contains(entry.hash())) {
                files.add(new FilePath(new String[]{"<html><font color=gray>&lt;unnamed&gt;</font></html>", Long.toHexString(entry.hash())}, entry.hash()));
            }
        }

        return super.loadChildren(monitor);
    }

    @NotNull
    @Override
    public String getLabel() {
        return packfile.getName();
    }

    @NotNull
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
}
