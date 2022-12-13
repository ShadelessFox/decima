package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "vector", name = "Vector", value = {
    @Type(name = "IVec2"), @Type(name = "Vec2"), @Type(name = "Vec2Pack"),
    @Type(name = "IVec3"), @Type(name = "Vec3"), @Type(name = "Vec3Pack"),
    @Type(name = "IVec4"), @Type(name = "Vec4"), @Type(name = "Vec4Pack")
})
public class VecValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            final Number[] elements = switch (type.getTypeName()) {
                case "IVec2", "Vec2", "Vec2Pack" -> new Number[]{obj.get("X"), obj.get("Y")};
                case "IVec3", "Vec3", "Vec3Pack" -> new Number[]{obj.get("X"), obj.get("Y"), obj.get("Z")};
                case "IVec4", "Vec4", "Vec4Pack" -> new Number[]{obj.get("X"), obj.get("Y"), obj.get("Z"), obj.get("W")};
                default -> null;
            };

            if (elements != null) {
                component.append("[", TextAttributes.REGULAR_ATTRIBUTES);

                for (int i = 0; i < elements.length; i++) {
                    component.append(elements[i].toString(), CommonTextAttributes.NUMBER_ATTRIBUTES);

                    if (i < elements.length - 1) {
                        component.append(", ", TextAttributes.REGULAR_ATTRIBUTES);
                    }
                }

                component.append("]", TextAttributes.REGULAR_ATTRIBUTES);
            }
        };
    }
}
