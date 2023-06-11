package com.shade.decima.ui.controls;

import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileExtensionFilter extends FileFilter {
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

    @Override
    public String getDescription() {
        return Arrays.stream(extensions)
            .map(ext -> "*." + ext)
            .collect(Collectors.joining(", ", description + " (", ")"));
    }
}
