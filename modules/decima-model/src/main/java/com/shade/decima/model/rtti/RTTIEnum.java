package com.shade.decima.model.rtti;

import com.shade.util.NotNull;

import java.util.Set;

public abstract class RTTIEnum extends RTTIType<RTTIEnum.Constant> {
    @NotNull
    public abstract Constant[] values();

    @NotNull
    public abstract Constant valueOf(int value);

    @NotNull
    public abstract Constant valueOf(@NotNull String value);

    public abstract boolean isEnumSet();

    public interface Constant {
        @NotNull
        RTTIEnum parent();

        @NotNull
        String name();

        int value();
    }

    public interface ConstantSet extends Constant {
        @NotNull
        Set<? extends Constant> constants();
    }
}
