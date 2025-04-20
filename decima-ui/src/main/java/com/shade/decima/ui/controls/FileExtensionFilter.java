package com.shade.decima.ui.controls;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileExtensionFilter extends FileFilter {
    /**
     * The dynamic library extension for the current platform.
     * <ul>
     *     <li>On macOS, the extension is {@code dylib}</li>
     *     <li>On Linux, the extension is {@code so}</li>
     *     <li>On Windows, the extension is {@code dll}</li>
     * </ul>
     */
    public static final String LIBRARY = SystemInfo.isMacOS ? "dylib" : SystemInfo.isLinux ? "so" : "dll";

    private final String description;
    private final String[] extensions;

    public FileExtensionFilter(@NotNull String description, @NotNull String... extensions) {
        if (extensions.length < 1) {
            throw new IllegalArgumentException("Extensions must not be empty");
        }

        this.description = description;
        this.extensions = extensions;
    }

    @Override
    public boolean accept(File file) {
        if (file == null) {
            return false;
        }

        if (file.isDirectory()) {
            // The file chooser takes the filter very serious and allows it to filter everything including directories
            // If validation is used, UIUtils#isValid would return `true` for a directory which is wrong
            return ReflectionUtils.wasInvokedFrom("javax.swing.JFileChooser", "accept", 2);
        }

        final String fileName = file.getName();
        final String fileExtension = IOUtils.getFullExtension(fileName);

        if (!fileExtension.isEmpty()) {
            for (String extension : extensions) {
                if (extension.equalsIgnoreCase(fileExtension)) {
                    return true;
                }
            }
        }

        return false;
    }

    @NotNull
    public String getExtension() {
        return extensions[0];
    }

    @Override
    public String getDescription() {
        return Arrays.stream(extensions)
            .map(ext -> "*." + ext)
            .collect(Collectors.joining(", ", description + " (", ")"));
    }
}
