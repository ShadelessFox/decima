package com.shade.decima.model.rtti.path;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.lang.reflect.Array;
import java.util.Objects;

public sealed interface RTTIPathElement {
    @NotNull
    Object get(@NotNull Object object);

    @NotNull
    Object get();

    void set(@NotNull Object object, @NotNull Object value);

    final class UUID implements RTTIPathElement {
        private final String uuid;
        private RTTIObject resolved;

        public UUID(@NotNull String uuid) {
            this.uuid = uuid;
        }

        public UUID(@NotNull RTTIObject entry) {
            this.uuid = RTTIUtils.uuidToString(entry.uuid());
            this.resolved = entry;
        }

        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return resolve((RTTICoreFile) object);
        }

        @NotNull
        @Override
        public RTTIObject get() {
            return Objects.requireNonNull(resolved, "Object is not resolved");
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        public String uuid() {
            return uuid;
        }

        @Override
        public String toString() {
            return uuid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UUID other = (UUID) o;
            return resolved != null && other.resolved != null
                ? resolved.equals(other.resolved)
                : uuid.equals(other.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        @NotNull
        private RTTIObject resolve(@NotNull RTTICoreFile file) {
            if (resolved == null) {
                for (RTTIObject entry : file.objects()) {
                    final String other = RTTIUtils.uuidToString(entry.uuid());

                    if (other.equals(uuid)) {
                        return resolved = entry;
                    }
                }

                throw new IllegalArgumentException("Can't find entry that matches the given UUID: " + uuid);
            }

            return resolved;
        }
    }

    final class Field implements RTTIPathElement {
        private final String name;
        private RTTIClass.Field<Object> resolved;

        public Field(@NotNull String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public Field(@NotNull RTTIClass.Field<?> field) {
            this.name = field.getName();
            this.resolved = (RTTIClass.Field<Object>) field;
        }

        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return resolve((RTTIObject) object).get((RTTIObject) object);
        }

        @NotNull
        @Override
        public RTTIClass.Field<Object> get() {
            return Objects.requireNonNull(resolved, "Field is not resolved");
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            resolve((RTTIObject) object).set((RTTIObject) object, value);
        }

        @NotNull
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field other = (Field) o;
            return resolved != null && other.resolved != null
                ? resolved.equals(other.resolved)
                : name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @NotNull
        private RTTIClass.Field<Object> resolve(@NotNull RTTIObject object) {
            if (resolved == null) {
                resolved = object.type().getField(name);
            }

            return resolved;
        }
    }

    record Index(int index) implements RTTIPathElement {
        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return Array.get(object, index);
        }

        @NotNull
        @Override
        public Object get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            Array.set(object, index, value);
        }

        @Override
        public String toString() {
            return String.valueOf(index);
        }
    }
}
