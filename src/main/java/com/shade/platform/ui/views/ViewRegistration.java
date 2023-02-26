package com.shade.platform.ui.views;

import javax.swing.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ViewRegistration {
    String id();

    String label();

    String icon() default "";

    String keystroke() default "";

    Anchor anchor() default Anchor.LEFT;

    int order() default Integer.MAX_VALUE;

    enum Anchor {
        LEFT,
        RIGHT,
        BOTTOM;

        public int toSwingConstant() {
            return switch (this) {
                case LEFT -> SwingConstants.LEFT;
                case RIGHT -> SwingConstants.RIGHT;
                case BOTTOM -> SwingConstants.BOTTOM;
            };
        }
    }
}
