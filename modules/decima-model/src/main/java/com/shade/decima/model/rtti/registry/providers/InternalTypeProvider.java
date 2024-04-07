package com.shade.decima.model.rtti.registry.providers;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InternalTypeProvider implements RTTITypeProvider {
    private static final Logger log = LoggerFactory.getLogger(InternalTypeProvider.class);

    private static final MethodType TYPE_CONSTRUCTOR = MethodType.methodType(void.class, String.class);
    private static final MethodType TYPE_PARAMETERIZED_CONSTRUCTOR = MethodType.methodType(void.class, String.class, RTTIType.class);

    private final Map<String, MethodHandle> types = new HashMap<>();
    private final Map<String, MethodHandle> templates = new HashMap<>();

    @Override
    public void initialize(@NotNull RTTIFactory factory, @NotNull ProjectContainer container) {
        final Set<Class<?>> types = ReflectionUtils.REFLECTIONS.getTypesAnnotatedWith(RTTIDefinition.class, true);

        for (Class<?> type : types) {
            final RTTIDefinition definition = type.getAnnotation(RTTIDefinition.class);

            if (!RTTIType.class.isAssignableFrom(type)) {
                log.error("Type don't extend RTTIType: " + type);
                continue;
            }

            if (definition.value().length == 0) {
                log.error("Type must have at least one name: " + type);
                continue;
            }

            if (RTTITypeParameterized.class.isAssignableFrom(type)) {
                for (String name : definition.value()) {
                    addParameterizedType(type, name);
                }
            } else {
                for (String name : definition.value()) {
                    addType(type, name);
                }
            }
        }
    }

    private void addType(@NotNull Class<?> type, @NotNull String name) {
        try {
            types.put(name, ReflectionUtils.LOOKUP.findConstructor(type, TYPE_CONSTRUCTOR));
        } catch (Throwable e) {
            log.error("Type " + type + " don't have a suitable constructor: " + TYPE_CONSTRUCTOR);
        }
    }

    private void addParameterizedType(@NotNull Class<?> type, @NotNull String name) {
        try {
            templates.put(name, ReflectionUtils.LOOKUP.findConstructor(type, TYPE_PARAMETERIZED_CONSTRUCTOR));
        } catch (Throwable e) {
            log.error("Parameterized type " + type + " don't have a suitable constructor: " + TYPE_PARAMETERIZED_CONSTRUCTOR);
        }
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTIFactory factory, @NotNull String name) {
        if (isTemplateTypeName(name)) {
            final String templateTypeName = getTemplateTypeName(name);
            final String templateParameterTypeName = getTemplateParameterTypeName(name);

            final MethodHandle templateTypeConstructor = templates.get(templateTypeName);
            final RTTIType<?> templateParameterType = factory.find(templateParameterTypeName, false);

            if (templateTypeConstructor == null) {
                throw new IllegalArgumentException("Type with name '" + templateTypeName + "' is either not templated or missing in the registry");
            }

            try {
                return (RTTIType<?>) templateTypeConstructor.invoke(templateTypeName, templateParameterType);
            } catch (Throwable e) {
                throw new RuntimeException("Error constructing parameterized RTTI type " + name, e);
            }
        }

        if (types.containsKey(name)) {
            try {
                return (RTTIType<?>) types.get(name).invoke(name);
            } catch (Throwable e) {
                throw new RuntimeException("Error constructing RTTI type " + name, e);
            }
        }

        return null;
    }

    private static boolean isTemplateTypeName(@NotNull String name) {
        return name.indexOf('<') > 0;
    }

    @NotNull
    private static String getTemplateTypeName(@NotNull String name) {
        final int start = name.indexOf('<');
        if (start <= 0) {
            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
        }
        return name.substring(0, start);
    }

    @NotNull
    private static String getTemplateParameterTypeName(@NotNull String name) {
        final int start = name.indexOf('<');
        final int end = name.lastIndexOf('>');
        if (start <= 0 || end < start + 1) {
            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
        }
        return name.substring(start + 1, end);
    }
}
