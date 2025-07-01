package com.shade.decima.ui.controls;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileExtensionFilter extends FileFilter {
    private final String description;
    private final String extension;
    private final Pattern pattern;

    /**
     * Creates a filter that matches native libraries of the current platform.
     * <ul>
     *     <li>On macOS, the extension is {@code dylib}</li>
     *     <li>On Linux, the extension is {@code so}, while also respecting versioned so-names</li>
     *     <li>On Windows, the extension is {@code dll}</li>
     * </ul>
     */
    @NotNull
    public static FileExtensionFilter ofNativeLibrary(@NotNull String description) {
        if (SystemInfo.isMacOS) {
            return new FileExtensionFilter(description, "dylib");
        } else if (SystemInfo.isLinux) {
            return new FileExtensionFilter(description, "so", Pattern.compile("so.*?"));
        } else {
            return new FileExtensionFilter(description, "dll");
        }
    }

    public FileExtensionFilter(@NotNull String description, @NotNull String extension) {
        this(description, extension, Pattern.compile(Pattern.quote(extension)));
    }

    public FileExtensionFilter(@NotNull String description, @NotNull String extension, @NotNull Pattern pattern) {
        this.description = description;
        this.extension = extension;
        this.pattern = pattern;
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

        return !fileExtension.isEmpty() && pattern.matcher(fileExtension).matches();
    }

    @NotNull
    public String getExtension() {
        return extension;
    }

    @Override
    public String getDescription() {
        return Stream.of(extension)
            .map(ext -> "*." + ext)
            .collect(Collectors.joining(", ", description + " (", ")"));
    }
}
