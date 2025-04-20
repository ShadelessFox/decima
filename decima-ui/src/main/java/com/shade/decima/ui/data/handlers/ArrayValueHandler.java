package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.stream.IntStream;

@ValueHandlerRegistration(value = {
    @Selector(type = @Type(type = byte[].class)),
    @Selector(type = @Type(type = short[].class)),
    @Selector(type = @Type(type = int[].class)),
    @Selector(type = @Type(type = long[].class)),
    @Selector(type = @Type(type = float[].class)),
    @Selector(type = @Type(type = double[].class)),
    @Selector(type = @Type(type = boolean[].class)),
    @Selector(type = @Type(type = Object[].class))
})
public class ArrayValueHandler implements ValueHandlerCollection<Object, RTTIPathElement.Index> {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("size = " + getArrayType(type).length(value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public RTTIPathElement.Index[] getElements(@NotNull RTTIType<?> type, @NotNull Object object) {
        return IntStream.range(0, getArrayType(type).length(object))
            .mapToObj(RTTIPathElement.Index::new)
            .toArray(RTTIPathElement.Index[]::new);
    }

    @NotNull
    @Override
    public String getElementName(@NotNull RTTIType<?> type, @NotNull Object object, @NotNull RTTIPathElement.Index element) {
        return String.valueOf(element.index());
    }

    @NotNull
    @Override
    public RTTIType<?> getElementType(@NotNull RTTIType<?> type, @NotNull Object object, @NotNull RTTIPathElement.Index element) {
        final RTTITypeArray<?> arrayType = getArrayType(type);
        if (arrayType.get(object, element.index()) instanceof RTTIObject obj) {
            return obj.type();
        } else {
            return arrayType.getComponentType();
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.arrayIcon");
    }

    @NotNull
    private static RTTITypeArray<?> getArrayType(@NotNull RTTIType<?> type) {
        return (RTTITypeArray<?>) type;
    }
}
