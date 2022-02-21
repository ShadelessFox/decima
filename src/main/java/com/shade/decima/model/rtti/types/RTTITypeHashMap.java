package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

@RTTIDefinition(name = "HashMap", aliases = {"HashSet"})
public class RTTITypeHashMap<T> extends RTTITypeContainer<RTTICollection<T>> {
    private final String name;
    private final RTTIType<T> type;

    public RTTITypeHashMap(@NotNull String name, @NotNull RTTIType<T> type) {
        this.name = name;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public RTTICollection<T> read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
        final T[] values = (T[]) Array.newInstance(type.getComponentType(), buffer.getInt());
        for (int i = 0; i < values.length; i++) {
            buffer.getInt();
            values[i] = type.read(registry, buffer);
        }
        return new RTTICollection<>(type, values);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTICollection<T> values) {
        throw new IllegalStateException("Not implemented");
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public Class<RTTICollection<T>> getComponentType() {
        return (Class<RTTICollection<T>>) (Object) RTTICollection.class;
    }

    @NotNull
    @Override
    public RTTIType<?> getContainedType() {
        return type;
    }
}
