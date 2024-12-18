package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.MODULE)
public @interface GenerateBindings {
    @interface Callback {
        /**
         * Name of the type for which the handler is registered
         */
        String type();

        /**
         * Handler class
         */
        Class<? extends ExtraBinaryDataCallback<?>> handler();
    }

    @interface Builtin {
        /**
         * Name of the type
         */
        String type();

        /**
         * Class that can represent that type at runtime
         */
        Class<?> javaType();
    }

    @interface Extension {
        /**
         * Name of the type to extend
         */
        String type();

        /**
         * Extension class
         */
        Class<?> extension();
    }

    /**
     * Path to the file containing type definitions ({@code .json})
     */
    String source();

    /**
     * A fully-qualified name of an interface under which the generated bindings will be placed
     */
    String target();

    /**
     * Collection of builtin types, such as numerics, strings, etc.
     */
    Builtin[] builtins() default {};

    /**
     * Collection of {@code MsgReadBinary} handlers
     */
    Callback[] callbacks() default {};

    /**
     * Collection of {@code Extension} handlers
     */
    Extension[] extensions() default {};
}
