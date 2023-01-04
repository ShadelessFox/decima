package com.shade.decima.ui.navigator.dnd;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
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
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileTransferable implements Transferable, Closeable {
    private final List<NavigatorFileNode> nodes;
    private volatile Data data;

    public FileTransferable(@NotNull List<NavigatorFileNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.javaFileListFlavor.equals(flavor);
    }

    @Override
    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            if (data == null) {
                data = Data.from(nodes);
            }

            return data.files;
        }

        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public synchronized void close() throws IOException {
        if (data != null) {
            data.delete();
            data = null;
        }
    }

    private record Data(@NotNull List<Path> paths, @NotNull List<File> files) {
        @NotNull
        public static Data from(@NotNull List<NavigatorFileNode> nodes) throws IOException {
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

            return new Data(paths, files);
        }

        public void delete() throws IOException {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
