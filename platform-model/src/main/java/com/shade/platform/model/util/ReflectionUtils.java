package com.shade.platform.model.util;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class ReflectionUtils {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // prevents instantiation
    }

    public static ScanResult scan() {
        return new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptModules("decima.*")
            .scan();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T, A extends Annotation> List<LazyWithMetadata<T, A>> findAnnotatedTypes(
        @NotNull Class<? extends T> annotatedType,
        @NotNull Class<? extends A> annotationType
    ) {
        List<Class<?>> types;

        try (ScanResult scanResult = scan()) {
            types = scanResult.getClassesWithAnnotation(annotationType).loadClasses();
        }

        List<LazyWithMetadata<T, A>> result = new ArrayList<>();

        for (Class<?> type : types) {
            final MethodHandle constructorHandle;

            if (!annotatedType.isAssignableFrom(type)) {
                log.error("{} can't be assigned to {}", type, annotatedType);
                continue;
            }

            try {
                constructorHandle = LOOKUP.findConstructor(type, MethodType.methodType(void.class));
            } catch (Throwable e) {
                log.error("Can't find suitable constructor for {}: {}", type, e.getMessage());
                continue;
            }

            try {
                final var metadata = type.getAnnotation(annotationType);
                final var supplier = (Supplier<T>) () -> {
                    try {
                        return (T) constructorHandle.invoke();
                    } catch (Throwable e) {
                        throw new IllegalStateException("Can't invoke constructor", e);
                    }
                };

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

    public static boolean wasInvokedFrom(@NotNull String className, @NotNull String methodName, int limit) {
        return wasInvokedFrom0((c, m) -> c.equals(className) && m.equals(methodName), limit);
    }

    public static boolean wasInvokedFrom(@NotNull BiPredicate<String, String> predicate, int limit) {
        return wasInvokedFrom0(predicate, limit);
    }

    private static boolean wasInvokedFrom0(@NotNull BiPredicate<String, String> predicate, int limit) {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        // getStackTrace() + wasInvokedFrom0() + wasInvokedFrom()
        for (int i = 3; i < elements.length && (limit <= 0 || i < limit + 3); i++) {
            final StackTraceElement element = elements[i];

            if (predicate.test(element.getClassName(), element.getMethodName())) {
                return true;
            }
        }

        return false;
    }
}
