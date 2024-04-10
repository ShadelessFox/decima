package com.shade.decima.model.rtti.registry.providers;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.registry.RTTITypeProvider;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class JavaTypeProvider implements RTTITypeProvider {
    @Override
    public void initialize(@NotNull RTTIFactory factory, @NotNull ProjectContainer container) {
        // nothing to initialize
    }

    @Nullable
    @Override
    public RTTIType<?> lookup(@NotNull RTTIFactory factory, @NotNull Class<?> type) {
        if (type.isArray()) {
            return new RTTITypeArray<>("Array", factory.find(type.getComponentType()));
        } else if (type.isPrimitive()) {
            if (type == Boolean.TYPE) {
                return factory.find("bool", false);
            } else if (type == Byte.TYPE) {
                return factory.find("int8", false);
            } else if (type == Short.TYPE) {
                return factory.find("int16", false);
            } else if (type == Integer.TYPE) {
                return factory.find("int32", false);
            } else if (type == Long.TYPE) {
                return factory.find("int64", false);
            } else if (type == Float.TYPE) {
                return factory.find("float", false);
            } else if (type == Double.TYPE) {
                return factory.find("double", false);
            } else {
                throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
            }
        } else {
            return new JavaClass(factory, type);
        }
    }

    private static class JavaClass extends RTTIClass {
        private final Class<?> type;
        private final Lazy<Superclass[]> superclasses;
        private final Lazy<Field<?>[]> declaredFields;
        private final Lazy<Field<?>[]> fields;

        public JavaClass(@NotNull RTTIFactory factory, @NotNull Class<?> type) {
            this.type = type;
            this.superclasses = Lazy.of(() -> collectSuperclasses(factory));
            this.declaredFields = Lazy.of(() -> collectFields(factory, true));
            this.fields = Lazy.of(() -> collectFields(factory, false));
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
        public RTTIObject create() {
            throw new NotImplementedException();
        }

        @NotNull
        @Override
        public RTTIObject copyOf(@NotNull RTTIObject value) {
            throw new IllegalStateException("Copying of Java classes is not supported");
        }

        @NotNull
        @Override
        public RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
            throw new IllegalStateException("Reading of Java classes is not supported");
        }

        @Override
        public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject value) {
            throw new IllegalStateException("Writing of Java classes is not supported");
        }

        @Override
        public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject value) {
            throw new IllegalStateException("Can't determine size of Java class");
        }

        @NotNull
        @Override
        public String getTypeName() {
            return type.getSimpleName();
        }

        @NotNull
        private Superclass[] collectSuperclasses(@NotNull RTTIFactory factory) {
            final List<Superclass> superclasses = new ArrayList<>();
            final RTTIExtends annotation = getClass().getDeclaredAnnotation(RTTIExtends.class);

            if (annotation != null && annotation.value().length > 0) {
                for (Type type : annotation.value()) {
                    final RTTIType<?> rttiType;

                    if (type.type() == Void.class) {
                        rttiType = factory.find(type.name());
                    } else {
                        rttiType = factory.find(type.type());
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
        private JavaField<?>[] collectFields(@NotNull RTTIFactory factory, boolean declared) {
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
                    type = factory.find(annotation.type().name());
                } else {
                    type = factory.find(annotation.type().type());
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
            handle.set(instance.data(), value);
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Nullable
        @Override
        public String getCategory() {
            return null;
        }

        @Override
        public int getOffset() {
            return Integer.MAX_VALUE;
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
