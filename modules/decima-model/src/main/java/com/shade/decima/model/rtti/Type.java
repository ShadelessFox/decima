package com.shade.decima.model.rtti;

import com.shade.decima.model.base.GameType;

public @interface Type {
    /**
     * A fully-qualified name of the {@link RTTIType}.
     */
    String name() default "";

    /**
     * A class that represents an instance type of the {@link RTTIType}.
     *
     * @see RTTIType#getInstanceType()
     */
    Class<?> type() default Void.class;

    /**
     * An array of supported games. Leave empty if game-agnostic.
     */
    GameType[] game() default {};
}
