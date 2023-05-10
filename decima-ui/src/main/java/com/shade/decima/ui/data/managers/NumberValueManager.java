package com.shade.decima.ui.data.managers;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.NumberValueEditor;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration(@Type(type = Number.class))
public class NumberValueManager implements ValueManager<Number> {
    @NotNull
    @Override
    public ValueEditor<Number> createEditor(@NotNull ValueController<Number> controller) {
        if (controller.getEditType() == ValueController.EditType.INLINE) {
            return new NumberValueEditor(controller);
        } else {
            throw new IllegalArgumentException("Unsupported edit type: " + controller.getEditType());
        }
    }

    @Override
    public boolean canEdit(@NotNull ValueController.EditType type) {
        return type == ValueController.EditType.INLINE;
    }
}
