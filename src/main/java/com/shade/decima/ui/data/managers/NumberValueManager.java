package com.shade.decima.ui.data.managers;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.ValueManagerRegistration;
import com.shade.decima.ui.data.editors.NumberValueEditor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueManagerRegistration(names = {"int"})
public class NumberValueManager implements ValueManager<Number> {
    @Nullable
    @Override
    public ValueEditor<Number> createEditor(@NotNull ValueController<Number> controller) {
        return switch (controller.getEditType()) {
            case INLINE -> new NumberValueEditor(controller, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case PANEL -> null;
        };
    }
}
