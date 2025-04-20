package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public abstract class BaseValueEditor<T, C extends JComponent> implements ValueEditor<T> {
    protected final MutableValueController<T> controller;
    protected C component;

    public BaseValueEditor(@NotNull MutableValueController<T> controller) {
        this.controller = controller;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        component = createComponentImpl();
        component.setInputVerifier(createInputValidator(component));
        return component;
    }

    @Override
    public boolean isEditorValueValid() {
        return UIUtils.isValid(component);
    }

    @Nullable
    protected InputValidator createInputValidator(@NotNull C component) {
        return null;
    }

    @NotNull
    protected abstract C createComponentImpl();

    protected class EditorInputValidator extends InputValidator {
        protected EditorInputValidator(@NotNull JComponent component) {
            super(component);
        }

        @NotNull
        @Override
        protected Validation validate(@NotNull JComponent input) {
            try {
                getEditorValue();
                return Validation.ok();
            } catch (Exception e) {
                return Validation.error(e.getMessage());
            }
        }
    }
}
