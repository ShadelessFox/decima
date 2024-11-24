package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: Move to modules

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PACKAGE)
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

    /**
     * Name of an interface under which the generated bindings will be placed
     */
    String namespace();

    /**
     * Path to the file containing type definitions ({@code .json})
     */
    String source();

    /**
     * Collection of builtin types, such as numerics, strings, etc.
     */
    Builtin[] builtins() default {};

    /**
     * Collection of {@code MsgReadBinary} handlers
     */
    Callback[] callbacks() default {};
}
