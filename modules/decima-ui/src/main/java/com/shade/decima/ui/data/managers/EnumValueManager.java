package com.shade.decima.ui.data.managers;

import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.MutableValueController.EditType;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.editors.EnumValueEditor;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueManagerRegistration;
import com.shade.util.NotNull;

@ValueManagerRegistration({
    @Selector(type = @Type(type = RTTITypeEnum.Constant.class))
})
public class EnumValueManager implements ValueManager<RTTITypeEnum.Constant> {
    @NotNull
    @Override
    public ValueEditor<RTTITypeEnum.Constant> createEditor(@NotNull MutableValueController<RTTITypeEnum.Constant> controller) {
        if (controller.getEditType() == EditType.INLINE) {
            return new EnumValueEditor(controller);
        } else {
            throw new IllegalArgumentException("Unsupported edit type: " + controller.getEditType());
        }
    }

    @Override
    public boolean canEdit(@NotNull EditType type) {
        return type == EditType.INLINE;
    }
}
