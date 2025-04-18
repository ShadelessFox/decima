package com.shade.decima.ui.data.viewer.shader.ffm;

import java.lang.foreign.MemorySegment;
import java.util.function.Function;

public record IID<T>(GUID guid, Function<MemorySegment, T> constructor) {
    public static <T> IID<T> of(String name, Function<MemorySegment, T> constructor) {
        return new IID<>(GUID.of(name), constructor);
    }
}
