package com.shade.decima.model.viewer.gl;

import com.shade.util.NotNull;

public abstract class Uniform<T> {
    protected final String name;
    protected final int location;
    protected final int program;

    protected Uniform(@NotNull String name, int program, int location) {
        this.name = name;
        this.location = location;
        this.program = program;
    }

    @NotNull
    public abstract T get();

    public abstract void set(@NotNull T value);

    @NotNull
    public String getName() {
        return name;
    }

    public int getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Uniform[name='" + name + "', location=" + location + ", value=" + get() + "]";
    }
}
