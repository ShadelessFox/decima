package com.shade.decima.model.rtti.path;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.lang.reflect.Array;

public sealed interface RTTIPathElement permits RTTIPathElement.Index, RTTIPathElement.Field, RTTIPathElement.UUID {
    @NotNull
    Object get(@NotNull Object object);

    void set(@NotNull Object object, @NotNull Object value);

    record UUID(@NotNull String uuid) implements RTTIPathElement {
        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            for (RTTIObject entry : ((CoreBinary) object).entries()) {
                final String other = GGUUIDValueHandler.toString(entry.obj("ObjectUUID"));

                if (other.equals(uuid)) {
                    return entry;
                }
            }

            throw new IllegalArgumentException("Can't find entry that matches the given UUID: " + uuid);
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            throw new NotImplementedException();
        }
    }

    record Field(@NotNull String name) implements RTTIPathElement {
        @NotNull
        @Override
        public Object get(@NotNull Object object) {
            return ((RTTIObject) object).get(name);
        }

        @Override
        public void set(@NotNull Object object, @NotNull Object value) {
            ((RTTIObject) object).set(name, value);
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
