package com.shade.decima.model.util;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeClass;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RTTIUtils {
    private RTTIUtils() {
    }

    @NotNull
    public static ClassBuilder newClassBuilder(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        return new ClassBuilder(registry, name);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T[] readCollection(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIType<T> type, int count) {
        final T[] values = (T[]) Array.newInstance(type.getInstanceType(), count);
        for (int i = 0; i < values.length; i++) {
            values[i] = type.read(registry, buffer);
        }
        return values;
    }

    public static class ClassBuilder {
        private final RTTITypeRegistry registry;
        private final String name;
        private final Map<String, RTTIType<?>> members;

        public ClassBuilder(@NotNull RTTITypeRegistry registry, @NotNull String name) {
            this.registry = registry;
            this.name = name;
            this.members = new LinkedHashMap<>();
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
            final RTTITypeClass clazz = new RTTITypeClass(name,
                new RTTITypeClass.Base[0],
                new RTTITypeClass.Member[members.size()],
                Collections.emptyMap(),
                0,
                0
            );

            final RTTITypeClass.Member[] members = this.members.entrySet().stream()
                .map(info -> new RTTITypeClass.Member(clazz, info.getValue(), info.getKey(), "", 0, 0))
                .toArray(RTTITypeClass.Member[]::new);

            System.arraycopy(members, 0, clazz.getMembers(), 0, members.length);

            return clazz;
        }
    }
}
