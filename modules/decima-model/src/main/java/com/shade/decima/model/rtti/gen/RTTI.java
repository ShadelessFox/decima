package com.shade.decima.model.rtti.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RTTI {
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Serializable {
        long hash();
    }

    @Target(ElementType.METHOD)
    public @interface Attr {
        String category() default "";

        int offset();

        int flags();
    }

    @Target({ElementType.TYPE_USE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Base {
        int offset();
    }

    public sealed interface Enum {
        String name();

        String[] aliases();

        non-sealed interface OfByte extends Enum {
            byte value();
        }

        non-sealed interface OfShort extends Enum {
            short value();
        }

        non-sealed interface OfInt extends Enum {
            int value();
        }
    }
}
