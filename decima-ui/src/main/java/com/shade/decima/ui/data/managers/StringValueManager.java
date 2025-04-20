package com.shade.decima.ui.data.managers;

import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.MutableValueController.EditType;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.StringValueEditor;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration({
    @Selector(type = @Type(type = String.class))
})
public class StringValueManager implements ValueManager<String> {
    @NotNull
    @Override
    public ValueEditor<String> createEditor(@NotNull MutableValueController<String> controller) {
        return new StringValueEditor(controller);
    }

    @Override
    public boolean canEdit(@NotNull EditType type) {
        return true;
    }
}
