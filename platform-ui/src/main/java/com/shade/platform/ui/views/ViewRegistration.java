package com.shade.platform.ui.views;

import com.shade.platform.model.ExtensionPoint;
import com.shade.util.NotNull;

import javax.swing.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(View.class)
public @interface ViewRegistration {
    String id();

    String label();

    String icon() default "";

    String keystroke() default "";

    Anchor anchor() default Anchor.LEFT;

    int order();

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

        @NotNull
        public static Anchor valueOf(int value) {
            return switch (value) {
                case SwingConstants.LEFT -> LEFT;
                case SwingConstants.RIGHT -> RIGHT;
                case SwingConstants.BOTTOM -> BOTTOM;
                default -> throw new IllegalArgumentException(String.valueOf(value));
            };
        }
    }
}
