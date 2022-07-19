package com.shade.decima.ui.editor.binary;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorController;
import com.shade.decima.ui.editor.EditorInput;

import javax.swing.*;

public class BinaryEditor implements Editor {
    private final EditorInput input;
    private final EditorController controller;
    private final JLabel placeholder;

    public BinaryEditor(@NotNull EditorInput input) {
        this.input = input;
        this.controller = new Controller();
        this.placeholder = new JLabel("Placeholder for binary editor", SwingConstants.CENTER);
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return placeholder;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @NotNull
    @Override
    public EditorController getController() {
        return controller;
    }

    private class Controller implements EditorController {
        @Nullable
        @Override
        public RTTIType<?> getSelectedType() {
            return null;
        }

        @Nullable
        @Override
        public Object getSelectedValue() {
            return null;
        }

        @Override
        public void setSelectedValue(@Nullable Object value) {
            // not implemented
        }

        @NotNull
        @Override
        public JComponent getFocusComponent() {
            return placeholder;
        }
    }

}
