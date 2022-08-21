package com.shade.decima.ui.controls.validators;

import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ExistingFileValidator extends InputValidator {
    private final FileFilter filter;

    public ExistingFileValidator(@NotNull JComponent component, @Nullable FileFilter filter) {
        super(component);
        this.filter = filter;
    }

    @NotNull
    @Override
    protected Validation validate(@NotNull JComponent input) {
        final File file = new File(((JTextField) input).getText());

        return file.exists() && (filter == null || filter.accept(file))
            ? Validation.ok()
            : Validation.error("File does not exist");
    }
}
