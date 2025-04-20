package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.editor.core.command.ValueChangeCommand;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

public record CoreValueController<T>(
    @NotNull CoreEditor editor,
    @NotNull CoreNodeObject node,
    @NotNull MutableValueController.EditType type
) implements MutableValueController<T> {
    @NotNull
    @Override
    public EditType getEditType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public RTTIType<T> getValueType() {
        return (RTTIType<T>) node.getType();
    }

    @NotNull
    @Override
    public RTTIPath getValuePath() {
        return node.getPath();
    }

    @NotNull
    @Override
    public String getValueLabel() {
        return node.getLabel();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return editor;
    }

    @NotNull
    @Override
    public Project getProject() {
        return editor.getInput().getProject();
    }

    @NotNull
    @Override
    public RTTICoreFile getCoreFile() {
        return editor.getCoreFile();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public T getValue() {
        return (T) node.getValue();
    }

    @Override
    public void setValue(@NotNull T newValue) {
        final T oldValue = getValue();

        if (!newValue.equals(oldValue)) {
            editor.getCommandManager().add(new ValueChangeCommand(editor.getTree(), node, oldValue, newValue));
        }
    }
}
