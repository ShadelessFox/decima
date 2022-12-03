package com.shade.decima.model.rtti.registry.providers;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class JavaTypeProvider implements RTTITypeProvider {
    @Override
    public void initialize(@NotNull RTTITypeRegistry registry, @NotNull ProjectContainer container) {
        // nothing to initialize
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull Class<?> type) {
        if (type.isArray()) {
            return new RTTITypeArray<>("Array", registry.find(type.getComponentType()));
        } else if (type.isPrimitive()) {
            if (type == Boolean.TYPE) {
                return registry.find("bool", false);
            } else if (type == Byte.TYPE) {
                return registry.find("int8", false);
            } else if (type == Short.TYPE) {
                return registry.find("int16", false);
            } else if (type == Integer.TYPE) {
                return registry.find("int32", false);
            } else if (type == Long.TYPE) {
                return registry.find("int64", false);
            } else if (type == Float.TYPE) {
                return registry.find("float", false);
            } else if (type == Double.TYPE) {
                return registry.find("double", false);
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
            }
        } else {
            return new JavaClass(registry, type);
        }
    }

    private static class JavaClass extends RTTIClass {
        private final Class<?> type;
        private final Lazy<Superclass[]> superclasses;
        private final Lazy<Field<?>[]> declaredFields;
        private final Lazy<Field<?>[]> fields;

        public JavaClass(@NotNull RTTITypeRegistry registry, @NotNull Class<?> type) {
            this.type = type;
            this.superclasses = Lazy.of(() -> collectSuperclasses(registry));
            this.declaredFields = Lazy.of(() -> collectFields(registry, true));
            this.fields = Lazy.of(() -> collectFields(registry, false));
        }

        @NotNull
        @Override
        public Superclass[] getSuperclasses() {
            return superclasses.get();
        }

        @NotNull
        @Override
        public Message<?>[] getMessages() {
            return new Message[0];
        }

        @NotNull
        @Override
        public Field<?>[] getDeclaredFields() {
            return declaredFields.get();
        }

        @NotNull
        @Override
        public Field<?>[] getFields() {
            return fields.get();
        }

        @NotNull
        @Override
        public RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            throw new IllegalStateException("Reading of Java classes is not supported");
        }

        @Override
        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject value) {
            throw new IllegalStateException("Writing of Java classes is not supported");
        }

        @Override
        public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject value) {
            throw new IllegalStateException("Can't determine size of Java class");
        }

        @NotNull
        @Override
        public String getTypeName() {
            return type.getSimpleName();
        }

        @NotNull
        private Superclass[] collectSuperclasses(@NotNull RTTITypeRegistry registry) {
            final List<Superclass> superclasses = new ArrayList<>();
            final RTTIExtends annotation = getClass().getDeclaredAnnotation(RTTIExtends.class);

            if (annotation != null && annotation.value().length > 0) {
                for (Type type : annotation.value()) {
                    final RTTIType<?> rttiType;

                    if (type.type() == Void.class) {
                        rttiType = registry.find(type.name());
                    } else {
                        rttiType = registry.find(type.type());
                    }

                    if (rttiType instanceof RTTIClass cls) {
                        superclasses.add(new JavaSuperclass(cls));
                    } else {
                        throw new IllegalArgumentException("RTTIExtends must contain only classes");
                    }
                }
            }

            // TODO: Check for the presence of super fields

            return superclasses.toArray(Superclass[]::new);
        }

        @NotNull
        private JavaField<?>[] collectFields(@NotNull RTTITypeRegistry registry, boolean declared) {
            final List<JavaField<?>> fields = new ArrayList<>();

            for (var field : (declared ? type.getDeclaredFields() : type.getFields())) {
                final RTTIField annotation = field.getDeclaredAnnotation(RTTIField.class);

                if (annotation == null || !Modifier.isPublic(field.getModifiers())) {
                    continue;
                }

                final String name;
                final RTTIType<?> type;
                final VarHandle handle;

                if (annotation.name().isEmpty()) {
                    name = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                } else {
                    name = annotation.name();
                }

                if (annotation.type().type() == Void.class) {
                    type = registry.find(annotation.type().name());
                } else {
                    type = registry.find(annotation.type().type());
                }

                try {
                    handle = ReflectionUtils.LOOKUP.unreflectVarHandle(field);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Can't unreflect field", e);
                }

                fields.add(new JavaField<>(this, type, name, handle));

            }

            return fields.toArray(JavaField[]::new);
        }
    }

    private record JavaSuperclass(@NotNull RTTIClass type) implements RTTIClass.Superclass {
        @NotNull
        @Override
        public RTTIClass getType() {
            return type;
        }
    }

    private record JavaField<T_VALUE>(
        @NotNull JavaClass parent,
        @NotNull RTTIType<T_VALUE> type,
        @NotNull String name,
        @NotNull VarHandle handle
    ) implements RTTIClass.Field<T_VALUE> {
        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public T_VALUE get(@NotNull RTTIObject instance) {
            return (T_VALUE) handle.get(instance.data());
        }

        @Override
        public void set(@NotNull RTTIObject instance, @NotNull T_VALUE value) {
            handle.set(instance, instance.data());
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public RTTIType<T_VALUE> getType() {
            return type;
        }

        @NotNull
        @Override
        public RTTIClass getParent() {
            return parent;
        }
    }
}
