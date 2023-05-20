package com.shade.decima.ui.data.managers;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.BoolValueEditor;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration({
    @Selector(type = @Type(type = Boolean.class))
})
public class BoolValueManager implements ValueManager<Boolean> {
    @NotNull
    @Override
    public ValueEditor<Boolean> createEditor(@NotNull ValueController<Boolean> controller) {
        if (controller.getEditType() == ValueController.EditType.INLINE) {
            return new BoolValueEditor(controller);
        } else {
            throw new IllegalArgumentException("Unsupported edit type: " + controller.getEditType());
        }
    }

    @Override
    public boolean canEdit(@NotNull ValueController.EditType type) {
        return type == ValueController.EditType.INLINE;
    }
}
