package com.shade.decima.ui.data.managers;

import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.EnumValueEditor;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueManagerRegistration(@Type(type = RTTITypeEnum.Constant.class))
public class EnumValueManager implements ValueManager<RTTITypeEnum.Constant> {
    @Nullable
    @Override
    public ValueEditor<RTTITypeEnum.Constant> createEditor(@NotNull ValueController<RTTITypeEnum.Constant> controller) {
        return switch (controller.getEditType()) {
            case INLINE -> new EnumValueEditor(controller);
            case PANEL -> null;
        };
    }
}
