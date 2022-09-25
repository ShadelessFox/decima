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
    private final boolean required;

    public ExistingFileValidator(@NotNull JComponent component, @Nullable FileFilter filter, boolean required) {
        super(component);
        this.filter = filter;
        this.required = required;

        verify(component);
    }

    public ExistingFileValidator(@NotNull JComponent component, @Nullable FileFilter filter) {
        this(component, filter, true);
    }

    @NotNull
    @Override
    protected Validation validate(@NotNull JComponent input) {
        final String text = ((JTextField) input).getText();

        if (text.isEmpty() && !required) {
            return Validation.ok();
        }

        final File file = new File(text);

        return file.exists() && (filter == null || filter.accept(file))
            ? Validation.ok()
            : Validation.error("File does not exist");
    }
}
