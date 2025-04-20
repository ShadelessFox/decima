package com.shade.decima.ui.data.managers;

import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.MutableValueController.EditType;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.NumberValueEditor;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration({
    @Selector(type = @Type(type = Number.class))
})
public class NumberValueManager implements ValueManager<Number> {
    @NotNull
    @Override
    public ValueEditor<Number> createEditor(@NotNull MutableValueController<Number> controller) {
        if (controller.getEditType() == EditType.INLINE) {
            return new NumberValueEditor(controller);
        } else {
            throw new IllegalArgumentException("Unsupported edit type: " + controller.getEditType());
        }
    }

    @Override
    public boolean canEdit(@NotNull EditType type) {
        return type == EditType.INLINE;
    }
}
