package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;

public @interface Type {
    /**
     * A name of an RTTI class this type represents.
     */
    String name() default "";

    /**
     * A class inheriting from {@link com.shade.decima.model.rtti.RTTIType} this type represents.
     */
    Class<?> type() default Void.class;

    /**
     * An array of supported games. Leave empty if game-agnostic.
     */
    GameType[] game() default {};
}
