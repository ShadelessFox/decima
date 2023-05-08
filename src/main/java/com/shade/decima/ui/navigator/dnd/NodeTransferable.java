package com.shade.decima.ui.navigator.dnd;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.util.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class NodeTransferable implements Transferable, Closeable {
    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
        DataFlavor.javaFileListFlavor,
        EditorStack.editorInputListFlavor
    };

    private final NavigatorFileNode[] nodes;
    private volatile TemporaryFiles files;

    public NodeTransferable(@NotNull NavigatorFileNode[] nodes) {
        this.nodes = nodes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor other) {
        for (DataFlavor flavor : flavors) {
            if (flavor.equals(other)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == DataFlavor.stringFlavor) {
            return Arrays.stream(nodes)
                .map(NavigatorFileNode::getLabel)
                .collect(Collectors.joining(System.lineSeparator()));
        }

        if (flavor == DataFlavor.javaFileListFlavor) {
            if (files == null) {
                files = TemporaryFiles.from(nodes);
            }

            return files.files;
        }

        if (flavor == EditorStack.editorInputListFlavor) {
            return Arrays.stream(nodes)
                .map(NodeEditorInputSimple::new)
                .toList();
        }

        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public void close() throws IOException {
        if (files != null) {
            files.delete();
            files = null;
        }
    }

    private record TemporaryFiles(@NotNull List<Path> paths, @NotNull List<File> files) {
        @NotNull
        public static TemporaryFiles from(@NotNull NavigatorFileNode[] nodes) throws IOException {
            final Path dir = Files.createTempDirectory("decima-dnd");
            final List<File> files = new ArrayList<>();
            final List<Path> paths = new ArrayList<>();

            for (NavigatorFileNode node : nodes) {
                final Packfile packfile = node.getPackfile();
                final Path file = dir.resolve(node.getLabel());

                try (InputStream is = packfile.newInputStream(node.getHash())) {
                    Files.copy(is, file, REPLACE_EXISTING);
                }

                files.add(file.toFile());
                paths.add(file);
            }

            paths.add(dir);

            return new TemporaryFiles(paths, files);
        }

        public void delete() throws IOException {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
