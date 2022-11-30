package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public abstract class RTTIClass<T_INSTANCE> extends RTTIType<T_INSTANCE> {
    @NotNull
    public static <T, R> R get(@NotNull RTTIClass<T> type, @NotNull T object, @NotNull String name) {
        return type.<R>getField(name).get(object);
    }

    public static <T> void set(@NotNull RTTIClass<T> type, @NotNull T object, @NotNull String name, @NotNull Object value) {
        type.getField(name).set(object, value);
    }

    @NotNull
    public abstract Superclass[] getSuperclasses();

    @NotNull
    public abstract Field<T_INSTANCE, ?>[] getDeclaredFields();

    @NotNull
    public abstract Field<T_INSTANCE, ?>[] getFields();

    @NotNull
    public abstract Message<?>[] getMessages();

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> Field<T_INSTANCE, T> getDeclaredField(@NotNull String name) {
        for (Field<T_INSTANCE, ?> field : getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return (Field<T_INSTANCE, T>) field;
            }
        }

        throw new IllegalArgumentException("Class '" + getTypeName() + "' has no declared field called '" + name + "'");
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> Field<T_INSTANCE, T> getField(@NotNull String name) {
        for (Field<T_INSTANCE, ?> field : getFields()) {
            if (field.getName().equals(name)) {
                return (Field<T_INSTANCE, T>) field;
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

    public boolean isInstanceOf(@NotNull RTTIClass<?> type) {
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
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull T_INSTANCE value) {
        int size = 0;

        for (Field<T_INSTANCE, ?> field : getFields()) {
            size += ((RTTIType<Object>) field.getType()).getSize(registry, field.get(value));
        }

        return size;
    }

    public interface Superclass {
        @NotNull
        RTTIClass<?> getType();
    }

    public interface Message<T_HANDLER extends MessageHandler> {
        @Nullable
        T_HANDLER getHandler();

        @NotNull
        String getName();
    }

    public interface Field<T_INSTANCE, T_VALUE> {
        @NotNull
        T_VALUE get(@NotNull T_INSTANCE object);

        void set(@NotNull T_INSTANCE object, T_VALUE value);

        @NotNull
        RTTIClass<T_INSTANCE> getParent();

        @NotNull
        RTTIType<T_VALUE> getType();

        @NotNull
        String getName();
    }
}
