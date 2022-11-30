package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class RTTIUtils {
    private RTTIUtils() {
    }

    @NotNull
    public static ClassBuilder newClassBuilder(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        return new ClassBuilder(registry, name);
    }

    @NotNull
    public static ClassBuilder newClassBuilder(@NotNull RTTITypeRegistry registry, @NotNull String base, @NotNull String name) {
        return new ClassBuilder(registry, (RTTITypeClass) registry.find(base), name);
    }

    @NotNull
    public static Object readCollection(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIType<?> type, int count) {
        final Object array = Array.newInstance(type.getInstanceType(), count);
        for (int i = 0; i < count; i++) {
            Array.set(array, i, type.read(registry, buffer));
        }
        return array;
    }

    public static class ClassBuilder {
        private final RTTITypeRegistry registry;
        private final String name;
        private final RTTITypeClass base;
        private final Map<String, RTTIType<?>> members;

        public ClassBuilder(@NotNull RTTITypeRegistry registry, @Nullable RTTITypeClass base, @NotNull String name) {
            this.registry = registry;
            this.name = name;
            this.base = base;
            this.members = new LinkedHashMap<>();
        }

        public ClassBuilder(@NotNull RTTITypeRegistry registry, @NotNull String name) {
            this(registry, null, name);
        }

        @NotNull
        public ClassBuilder member(@NotNull String name, @NotNull String type) {
            return member(name, registry.find(type));
        }

        @NotNull
        public ClassBuilder member(@NotNull String name, @NotNull RTTIType<?> type) {
            members.put(name, type);
            return this;
        }

        @NotNull
        public RTTITypeClass build() {
            final RTTITypeClass clazz = new RTTITypeClass(name, 0, 0);

            clazz.setSuperclasses(base != null ? new RTTITypeClass.MySuperclass[]{new RTTITypeClass.MySuperclass(base, 0)} : new RTTITypeClass.MySuperclass[0]);
            clazz.setMessages(new RTTIClass.Message[0]);
            clazz.setFields(members.entrySet().stream()
                .map(info1 -> new RTTITypeClass.MyField(clazz, info1.getValue(), info1.getKey(), "", 0, 0))
                .toArray(RTTITypeClass.MyField[]::new));

            return clazz;
        }
    }
}
