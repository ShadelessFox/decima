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
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NavigatorPackfileNode extends NavigatorLazyNode {
    private static final Logger log = LoggerFactory.getLogger(NavigatorPackfileNode.class);

    private final Project project;
    private final Packfile packfile;

    public NavigatorPackfileNode(@NotNull NavigatorProjectNode parent, @NotNull Packfile packfile) {
        super(parent);
        this.project = parent.getProject();
        this.packfile = packfile;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final PackfileManager manager = project.getPackfileManager();
        final Packfile prefetch = manager.findAny("prefetch/fullgame.prefetch");

        if (prefetch == null) {
            log.error("Can't find prefetch file");
            return new NavigatorNode[0];
        }

        final CoreObject root = CoreObject.from(prefetch.extract("prefetch/fullgame.prefetch"), project.getTypeRegistry());

        if (root.isEmpty()) {
            log.error("Prefetch file is empty");
            return new NavigatorNode[0];
        }

        final RTTIObject object = root.getEntries().get(0);
        final List<NavigatorNode> children = new ArrayList<>();
        final Set<Long> containing = new HashSet<>();

        for (RTTIObject file : object.<RTTICollection<RTTIObject>>get("Files")) {
            final String path = PackfileBase.getNormalizedPath(file.get("Path"));
            final long hash = PackfileBase.getPathHash(path);

            if (packfile.contains(hash)) {
                children.add(new NavigatorFileNode(this, path.split("/"), hash));
                containing.add(hash);
            }
        }

        for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
            if (!containing.contains(entry.hash())) {
                children.add(new NavigatorFileNode(this, new String[]{"<html><font color=gray>&lt;unnamed&gt;</font></html>", Long.toHexString(entry.hash())}, entry.hash()));
            }
        }

        children.sort(Comparator.comparing(NavigatorNode::getLabel));

        return children.toArray(NavigatorNode[]::new);
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
}
