package com.shade.platform.model.util;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ReflectionUtils {
    public static final Reflections REFLECTIONS = new Reflections("com.shade");
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // prevents instantiation
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T, A extends Annotation> List<LazyWithMetadata<T, A>> findAnnotatedTypes(
        @NotNull Class<? extends T> annotatedType,
        @NotNull Class<? extends A> annotationType
    ) {
        final Set<Class<?>> types = REFLECTIONS.getTypesAnnotatedWith(annotationType);

        if (types.isEmpty()) {
            return List.of();
        }

        final MethodType constructorType = MethodType.methodType(void.class);
        final MethodType supplierType = MethodType.methodType(Supplier.class);
        final MethodType supplierGetType = MethodType.methodType(annotatedType);
        final List<LazyWithMetadata<T, A>> result = new ArrayList<>();

        for (Class<?> type : types) {
            final MethodHandle constructorHandle;

            if (!annotatedType.isAssignableFrom(type)) {
                log.error(type + " can't be assigned to " + annotatedType);
                continue;
            }

            try {
                constructorHandle = LOOKUP.findConstructor(type, constructorType);
            } catch (Throwable e) {
                log.error("Can't find suitable constructor for " + type);
                continue;
            }

            try {
                final var metadata = type.getAnnotation(annotationType);
                final var supplier = (Supplier<T>) LambdaMetafactory
                    .metafactory(LOOKUP, "get", supplierType, supplierGetType.generic(), constructorHandle, supplierGetType)
                    .getTarget()
                    .invokeExact();

                result.add(LazyWithMetadata.of(supplier, metadata, (Class<? extends T>) type));
            } catch (Throwable e) {
                log.error("Error constructing supplier handle", e);
            }
        }

        return result;
    }

    @Nullable
    public static Object handleObjectMethod(@NotNull Object proxy, @NotNull Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "Proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        };
    }
}
