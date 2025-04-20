package com.shade.platform.model;

import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.*;

public class ExtensionRegistry {
    private static final ExtensionRegistry INSTANCE = new ExtensionRegistry();

    private final Map<Class<? extends Annotation>, LazyWithMetadata<Object, ? extends Annotation>[]> extensionPoints;

    @SuppressWarnings("unchecked")
    private ExtensionRegistry() {
        List<Class<?>> types;

        try (ScanResult scanResult = ReflectionUtils.scan()) {
            types = scanResult.getAllAnnotations()
                .filter(x -> x.hasAnnotation(ExtensionPoint.class))
                .loadClasses();
        }

        Map<Class<? extends Annotation>, LazyWithMetadata<Object, ? extends Annotation>[]> extensions = new HashMap<>();

        for (Class<?> type : types) {
            var extension = type.getDeclaredAnnotation(ExtensionPoint.class);
            var extensionType = (Class<? extends Annotation>) type;
            var elements = ReflectionUtils.findAnnotatedTypes(extension.value(), extensionType);
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

        return Arrays.stream(extensions)
            .filter(extension -> extensionType.isAssignableFrom(extension.type()))
            .map(extension -> (LazyWithMetadata<T, A>) extension)
            .toList();
    }
}
