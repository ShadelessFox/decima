package com.shade.decima.ui.data.managers;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.EnumValueEditor;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration(@Type(type = RTTITypeEnum.Constant.class))
public class EnumValueManager implements ValueManager<RTTITypeEnum.Constant> {
    @NotNull
    @Override
    public ValueEditor<RTTITypeEnum.Constant> createEditor(@NotNull ValueController<RTTITypeEnum.Constant> controller) {
        if (controller.getEditType() == ValueController.EditType.INLINE) {
            return new EnumValueEditor(controller);
        } else {
            throw new IllegalArgumentException("Unsupported edit type: " + controller.getEditType());
        }
    }

    @Override
    public boolean canEdit(@NotNull ValueController.EditType type) {
        return type == ValueController.EditType.INLINE;
    }
}
