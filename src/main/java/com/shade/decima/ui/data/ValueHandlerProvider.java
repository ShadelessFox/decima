package com.shade.decima.ui.data;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.*;
import com.shade.decima.ui.data.handlers.*;
import com.shade.util.NotNull;

// TODO: Use services instead
public final class ValueHandlerProvider {
    private ValueHandlerProvider() {
    }

    @NotNull
    public static ValueHandler getValueHandler(@NotNull RTTIType<?> type, @NotNull GameType gameType) {
        final String name = type.getTypeName();

        if (name.equals("GGUUID")) {
            return GGUUIDValueHandler.INSTANCE;
        } else if (name.equals("bool")) {
            return BooleanValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeClass cls) {
            return cls.isInstanceOf(gameType == GameType.HZD ? "Resource" : "ResourceWithName") ? ObjectWithNameValueHandler.INSTANCE : ObjectValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeEnum || type instanceof RTTITypeEnumFlags) {
            return EnumValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeArray) {
            return ArrayValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeString) {
            return StringValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeReference) {
            return ReferenceValueHandler.INSTANCE;
        } else if (type instanceof RTTITypeNumber<?> t) {
            return t.isSigned() ? SignedNumberValueHandler.INSTANCE : UnsignedNumberValueHandler.INSTANCE;
        } else {
            return DefaultValueHandler.INSTANCE;
        }
    }
}
