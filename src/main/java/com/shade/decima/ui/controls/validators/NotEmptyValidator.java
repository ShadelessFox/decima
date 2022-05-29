package com.shade.decima.ui.controls.validators;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.validation.InputValidator;
import com.shade.decima.ui.controls.validation.Validation;

import javax.swing.*;

public class NotEmptyValidator extends InputValidator {
    public NotEmptyValidator(@NotNull JComponent component) {
        super(component);
    }

    @NotNull
    @Override
    protected Validation validate(@NotNull JComponent input) {
        return ((JTextField) input).getText().isBlank()
            ? Validation.error("This field must not be empty")
            : Validation.ok();
    }
}
