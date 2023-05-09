package com.shade.platform.model;

import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;

import java.lang.annotation.Annotation;
import java.util.*;

public class ExtensionRegistry {
    private static final ExtensionRegistry INSTANCE = new ExtensionRegistry();

    private final Map<Class<? extends Annotation>, LazyWithMetadata<Object, ? extends Annotation>[]> extensionPoints;

    @SuppressWarnings("unchecked")
    private ExtensionRegistry() {
        final Set<Class<?>> types = ReflectionUtils.REFLECTIONS.getTypesAnnotatedWith(ExtensionPoint.class, true);
        final Map<Class<? extends Annotation>, LazyWithMetadata<Object, ? extends Annotation>[]> extensions = new HashMap<>();

        for (Class<?> type : types) {
            final var extension = type.getDeclaredAnnotation(ExtensionPoint.class);
            final var extensionType = (Class<? extends Annotation>) type;
            final var elements = ReflectionUtils.findAnnotatedTypes(extension.value(), extensionType);
            extensions.put(extensionType, elements.toArray(LazyWithMetadata[]::new));
        }

        this.extensionPoints = Collections.unmodifiableMap(extensions);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T, A extends Annotation> List<LazyWithMetadata<T, A>> getExtensions(
        @NotNull Class<? extends T> extensionType,
        @NotNull Class<? extends A> extensionPoint
    ) {
        final ExtensionPoint metadata = extensionPoint.getDeclaredAnnotation(ExtensionPoint.class);

        if (metadata == null) {
            throw new IllegalArgumentException("Not an extension point: " + extensionPoint);
        }

        if (!metadata.value().isAssignableFrom(extensionType)) {
            throw new IllegalArgumentException("Extension point " + extensionPoint + " is incompatible with " + extensionType);
        }

        final LazyWithMetadata<Object, ? extends Annotation>[] extensions = INSTANCE.extensionPoints.get(extensionPoint);

        if (extensions == null) {
            throw new IllegalArgumentException("Can't find extension point " + extensionPoint);
        }

        return List.of((LazyWithMetadata<T, A>[]) extensions);
    }
}
