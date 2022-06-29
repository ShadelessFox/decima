package com.shade.decima.ui.navigator.dnd;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileTransferable implements Transferable, Closeable {
    private final Path directory;
    private final List<Path> files;

    public FileTransferable(@NotNull List<NavigatorFileNode> nodes) throws IOException {
        this.directory = Files.createTempDirectory("decima-dnd");
        this.files = new ArrayList<>();

        for (NavigatorFileNode node : nodes) {
            final Packfile packfile = UIUtils.getPackfile(node);
            final Path file = Files.createFile(directory.resolve(node.getName()));
            final byte[] bytes = packfile.extract(node.getHash());

            Files.write(file, bytes);
            files.add(file);
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.javaFileListFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
            return files.stream().map(Path::toFile).toList();
        }

        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public void close() throws IOException {
        for (Path file : files) {
            Files.delete(file);
        }

        Files.delete(directory);
    }
}
