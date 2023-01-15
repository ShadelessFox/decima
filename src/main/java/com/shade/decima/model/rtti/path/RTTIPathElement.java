package com.shade.decima.model.rtti.path;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.util.NotNull;

import java.lang.reflect.Array;

public sealed interface RTTIPathElement {
    @NotNull
    Object get(@NotNull Object object);

    void set(@NotNull Object object, @NotNull Object value);

    final class UUID implements RTTIPathElement {
        private final String uuid;
        private RTTIObject resolved;

        public UUID(@NotNull String uuid) {
            this.uuid = uuid;
        }

        public UUID(@NotNull RTTIObject entry) {
            this.uuid = GGUUIDValueHandler.toString(entry.obj("ObjectUUID"));
            this.resolved = entry;
        }

        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return resolve((CoreBinary) object);
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        public String uuid() {
            return uuid;
        }

        @NotNull
        private RTTIObject resolve(@NotNull CoreBinary object) {
            if (resolved == null) {
                for (RTTIObject entry : object.entries()) {
                    final String other = GGUUIDValueHandler.toString(entry.obj("ObjectUUID"));

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

        public Field(@NotNull RTTIClass.Field<Object> field) {
            this.name = field.getName();
            this.resolved = field;
        }

        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return resolve((RTTIObject) object).get((RTTIObject) object);
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            resolve((RTTIObject) object).set((RTTIObject) object, value);
        }

        @NotNull
        public String name() {
            return name;
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

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            Array.set(object, index, value);
        }
    }
}
