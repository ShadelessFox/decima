package com.shade.decima.ui.data.managers;

import com.shade.decima.ui.data.*;
import com.shade.decima.ui.data.editors.StringValueEditor;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration({@Type(name = "String"), @Type(name = "WString")})
public class StringValueManager implements ValueManager<String> {
    @NotNull
    @Override
    public ValueEditor<String> createEditor(@NotNull ValueController<String> controller) {
        return new StringValueEditor(controller);
    }
}
