package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.PathElement;
import com.shade.decima.model.rtti.path.PathElementField;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ValueHandlerRegistration(value = @Type(type = RTTIClass.class), order = 1000)
public class ObjectValueHandler implements ValueHandlerCollection<RTTIObject, RTTIClass.Field<Object>> {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Collection<RTTIClass.Field<Object>> getChildren(@NotNull RTTIType<?> type, @NotNull RTTIObject object) {
        final List<RTTIClass.Field<Object>> fields = new ArrayList<>();
        for (RTTIClass.Field<?> field : ((RTTIClass) type).getFields()) {
            if (field.get(object) != null) {
                fields.add((RTTIClass.Field<Object>) field);
            }
        }
        return fields;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIClass.Field<Object> field) {
        return field.getName();
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIClass.Field<Object> field) {
        return field.get(object);
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIClass.Field<Object> field) {
        return field.getType();
    }

    @NotNull
    @Override
    public PathElement getChildElement(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTIClass.Field<Object> field) {
        return new PathElementField(field);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.objectIcon");
    }
}
