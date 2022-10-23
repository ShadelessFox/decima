package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.PathElement;
import com.shade.decima.model.rtti.path.PathElementIndex;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.stream.IntStream;

public class ArrayValueHandler implements ValueHandlerCollection<Object, Integer> {
    public static final ArrayValueHandler INSTANCE = new ArrayValueHandler();

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append("size = " + getArrayType(type).length(value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
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
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        return getArrayType(type).get(array, index);
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        return getArrayType(type).getComponentType();
    }

    @NotNull
    @Override
    public PathElement getChildElement(@NotNull RTTIType<?> type, @NotNull Object array, @NotNull Integer index) {
        return new PathElementIndex(index);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.arrayIcon");
    }

    @NotNull
    private static RTTITypeArray<?> getArrayType(@NotNull RTTIType<?> type) {
        return (RTTITypeArray<?>) type;
    }
}
