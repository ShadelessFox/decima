package com.shade.decima.rtti;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RTTI {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Attr {
        String name();

        int offset();

        int flags() default 0;

        String min() default "";

        String max() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface Base {
        int offset();
    }

    public sealed interface Value {
        non-sealed interface OfByte extends Value {
            byte value();
        }

        non-sealed interface OfShort extends Value {
            short value();
        }

        non-sealed interface OfInt extends Value {
            int value();
        }
    }

    private RTTI() {
    }
}
