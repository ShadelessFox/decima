package com.shade.decima.ui.handler;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.*;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.handler.impl.*;

// TODO: Use services instead
public final class ValueHandlerProvider {
    private ValueHandlerProvider() {
    }

    @NotNull
    public static ValueHandler getValueHandler(@NotNull RTTIType<?> type) {
        final String name = type.getTypeName();

        if (name.equals("GGUUID")) {
            return GGUUIDValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeClass) {
            return ObjectValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeArray || name.equals("HashSet")) {
            return ArrayValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeHashMap) {
            return HashMapValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeString) {
            return StringValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeReference) {
            return ReferenceValueHandler.INSTANCE;
        } else if (name.startsWith("uint")) {
            return UnsignedNumberValueHandler.INSTANCE;
        } else if (name.contains("int") || name.contains("float") || name.contains("double")) {
            return SignedNumberValueHandler.INSTANCE;
        } else {
            return DefaultValueHandler.INSTANCE;
        }
    }
}
