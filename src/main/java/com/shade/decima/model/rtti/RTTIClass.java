package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public abstract class RTTIClass extends RTTIType<RTTIObject> {
    @NotNull
    public static <R> R get(@NotNull RTTIClass type, @NotNull RTTIObject object, @NotNull String name) {
        return type.<R>getField(name).get(object);
    }

    public static void set(@NotNull RTTIClass type, @NotNull RTTIObject object, @NotNull String name, @NotNull Object value) {
        type.getField(name).set(object, value);
    }

    @NotNull
    public abstract Superclass[] getSuperclasses();

    @NotNull
    public abstract Field<?>[] getDeclaredFields();

    @NotNull
    public abstract Field<?>[] getFields();

    @NotNull
    public abstract Message<?>[] getMessages();

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> Field<T> getDeclaredField(@NotNull String name) {
        for (Field<?> field : getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return (Field<T>) field;
            }
        }

        throw new IllegalArgumentException("Class '" + getTypeName() + "' has no declared field called '" + name + "'");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> Field<T> getField(@NotNull String name) {
        for (Field<?> field : getFields()) {
            if (field.getName().equals(name)) {
                return (Field<T>) field;
            }
        }

        throw new IllegalArgumentException("Class '" + getTypeName() + "' has no field called '" + name + "'");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends MessageHandler> Message<T> getMessage(@NotNull String name) {
        for (Message<?> message : getMessages()) {
            if (message.getName().equals(name)) {
                return (Message<T>) message;
            }
        }

        return null;
    }

    public boolean isInstanceOf(@NotNull String type) {
        if (getTypeName().equals(type)) {
            return true;
        }

        for (Superclass superclass : getSuperclasses()) {
            if (superclass.getType().isInstanceOf(type)) {
                return true;
            }
        }

        return false;
    }

    public boolean isInstanceOf(@NotNull RTTIClass type) {
        if (this == type) {
            return true;
        }

        for (Superclass superclass : getSuperclasses()) {
            if (superclass.getType().isInstanceOf(type)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject value) {
        int size = 0;

        for (Field<?> field : getFields()) {
            size += ((RTTIType<Object>) field.getType()).getSize(registry, field.get(value));
        }

        return size;
    }

    @NotNull
    @Override
    public Class<RTTIObject> getInstanceType() {
        return RTTIObject.class;
    }

    public interface Superclass {
        @NotNull
        RTTIClass getType();
    }

    public interface Message<T_HANDLER extends MessageHandler> {
        @Nullable
        T_HANDLER getHandler();

        @NotNull
        String getName();
    }

    public interface Field<T_VALUE> {
        T_VALUE get(@NotNull RTTIObject object);

        void set(@NotNull RTTIObject object, T_VALUE value);

        @NotNull
        RTTIClass getParent();

        @NotNull
        RTTIType<T_VALUE> getType();

        @NotNull
        String getName();
    }
}
