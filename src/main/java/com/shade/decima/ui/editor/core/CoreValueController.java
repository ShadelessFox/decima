package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.editor.core.command.AttributeChangeCommand;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

public record CoreValueController(
    @NotNull CoreEditor editor,
    @NotNull ValueManager<Object> manager,
    @NotNull CoreNodeObject node,
    @NotNull ValueController.EditType type
) implements ValueController<Object> {
    @NotNull
    @Override
    public EditType getEditType() {
        return type;
    }

    @NotNull
    @Override
    public ValueManager<Object> getValueManager() {
        return manager;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public RTTIType<Object> getValueType() {
        return (RTTIType<Object>) node.getType();
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
    public Object getValue() {
        return node.getValue();
    }

    @Override
    public void setValue(@NotNull Object newValue) {
        final Object oldValue = getValue();

        if (!newValue.equals(oldValue)) {
            editor.getCommandManager().add(new AttributeChangeCommand(editor.getTree(), node, oldValue, newValue));
        }
    }
}
