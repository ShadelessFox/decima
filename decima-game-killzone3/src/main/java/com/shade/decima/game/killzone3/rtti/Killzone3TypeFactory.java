package com.shade.decima.game.killzone3.rtti;

import com.shade.decima.rtti.factory.AbstractTypeFactory;
import com.shade.decima.rtti.factory.TypeId;
import com.shade.decima.rtti.runtime.TypeInfo;
import com.shade.util.NotNull;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class Killzone3TypeFactory extends AbstractTypeFactory {
    public Killzone3TypeFactory() {
        super(Killzone3.class, MethodHandles.lookup());
    }

    @NotNull
    @Override
    protected TypeId computeTypeId(@NotNull TypeInfo info) {
        return new Killzone3TypeId(info.name().fullName());
    }

    @Override
    protected void sortSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        // Attributes are not sorted
    }

    @Override
    protected void filterSerializableAttrs(@NotNull List<OrderedAttr> attrs) {
        // Remove save state attribute
        attrs.removeIf(attr -> (attr.info().flags() & 2) != 0);
        // Remove non-"serializable" attributes. They include holders for MsgReadBinary data
        attrs.removeIf(attr -> !attr.serializable());
    }
}
