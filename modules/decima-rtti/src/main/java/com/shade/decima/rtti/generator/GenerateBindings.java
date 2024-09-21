package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

    /**
     * Name of an interface under which the generated bindings will be placed
     */
    String namespace();

    /**
     * Path to the file containing type definitions ({@code .json})
     */
    String source();

    /**
     * Collection of {@code MsgReadBinary} handlers
     */
    Callback[] callbacks() default {};
}
