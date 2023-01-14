package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.stream.IntStream;

@ValueHandlerRegistration({
    @Type(type = byte[].class),
    @Type(type = short[].class),
    @Type(type = int[].class),
    @Type(type = long[].class),
    @Type(type = float[].class),
    @Type(type = double[].class),
    @Type(type = boolean[].class),
    @Type(type = Object[].class)
})
public class ArrayValueHandler implements ValueHandlerCollection<Object, Integer> {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("size = " + getArrayType(type).length(value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public Collection<Integer> getChildren(@NotNull RTTIType<?> type, @NotNull Object array) {
        return IntStream.range(0, getArrayType(type).length(array)).boxed().toList();
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        return String.valueOf(index);
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        final RTTITypeArray<?> arrayType = getArrayType(type);
        if (arrayType.get(array, index) instanceof RTTIObject obj) {
            return obj.type();
        } else {
            return arrayType.getComponentType();
        }
    }

    @NotNull
    @Override
    public RTTIPathElement getChildElement(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        return new RTTIPathElement.Index(index);
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
