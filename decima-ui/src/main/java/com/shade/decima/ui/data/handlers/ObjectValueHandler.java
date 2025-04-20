package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.util.AlphanumericComparator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;

@ValueHandlerRegistration(order = 1000, value = {
    @Selector(type = @Type(type = RTTIObject.class))
})
public class ObjectValueHandler implements ValueHandlerCollection<RTTIObject, RTTIPathElement.Field> {
    private static final Comparator<RTTIClass.Field<?>> FIELD_COMPARATOR = Comparator
        .comparingInt((RTTIClass.Field<?> field) -> field.getOffset())
        .thenComparing(RTTIClass.Field::getCategory, Comparator.nullsFirst(AlphanumericComparator.getInstance()))
        .thenComparing(RTTIClass.Field::getName, AlphanumericComparator.getInstance());

    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return null;
    }

    @NotNull
    @Override
    public RTTIPathElement.Field[] getElements(@NotNull RTTIType<?> type, @NotNull RTTIObject object) {
        return Arrays.stream(((RTTIClass) type).getFields())
            .filter(field -> field.get(object) != null)
            .sorted(FIELD_COMPARATOR)
            .map(RTTIPathElement.Field::new)
            .toArray(RTTIPathElement.Field[]::new);
    }

    @NotNull
    @Override
    public String getElementName(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIPathElement.Field element) {
        final RTTIClass.Field<Object> field = element.get();

        if (field.getCategory() != null) {
            for (RTTIClass.Field<?> other : object.type().getFields()) {
                if (other != field && other.getName().equals(field.getName())) {
                    return field.getCategory() + '.' + field.getName();
                }
            }
        }

        return field.getName();
    }

    @NotNull
    @Override
    public RTTIType<?> getElementType(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIPathElement.Field element) {
        if (element.get(object) instanceof RTTIObject obj) {
            return obj.type();
        } else {
            return element.get().getType();
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.objectIcon");
    }
}
