package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;

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
