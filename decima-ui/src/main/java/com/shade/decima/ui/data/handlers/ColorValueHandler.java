package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.model.util.MathUtils;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.*;

@ValueHandlerRegistration(id = "color", name = "Color", value = {
    @Selector(type = @Type(name = "RGBAColor")),
    @Selector(type = @Type(name = "RGBAColorRev")),
    @Selector(type = @Type(name = "FRGBAColor")),
    @Selector(type = @Type(name = "FRGBColor"))
})
public class ColorValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return new Decorator() {
            @Override
            public void decorate(@NotNull Object value, @NotNull ColoredComponent component) {
                final RTTIObject obj = (RTTIObject) value;

                final Color color = switch (type.getTypeName()) {
                    case "RGBAColor", "RGBAColorRev" -> new Color(
                        obj.i8("R") & 0xff,
                        obj.i8("G") & 0xff,
                        obj.i8("B") & 0xff,
                        obj.i8("A") & 0xff
                    );
                    case "FRGBAColor" -> new Color(
                        MathUtils.clamp(obj.f32("R"), 0.0f, 1.0f),
                        MathUtils.clamp(obj.f32("G"), 0.0f, 1.0f),
                        MathUtils.clamp(obj.f32("B"), 0.0f, 1.0f),
                        MathUtils.clamp(obj.f32("A"), 0.0f, 1.0f)
                    );
                    case "FRGBColor" -> new Color(
                        MathUtils.clamp(obj.f32("R"), 0.0f, 1.0f),
                        MathUtils.clamp(obj.f32("G"), 0.0f, 1.0f),
                        MathUtils.clamp(obj.f32("B"), 0.0f, 1.0f)
                    );
                    default -> null;
                };

                if (color != null) {
                    component.setTrailingIcon(new ColorIcon(color));
                }
            }

            @Override
            public boolean needsGap() {
                return false;
            }
        };
    }
}
