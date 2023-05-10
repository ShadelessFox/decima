package com.shade.decima.ui.data.managers;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.StringValueEditor;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration(@Type(type = String.class))
public class StringValueManager implements ValueManager<String> {
    @NotNull
    @Override
    public ValueEditor<String> createEditor(@NotNull ValueController<String> controller) {
        return new StringValueEditor(controller);
    }

    @Override
    public boolean canEdit(@NotNull ValueController.EditType type) {
        return true;
    }
}
