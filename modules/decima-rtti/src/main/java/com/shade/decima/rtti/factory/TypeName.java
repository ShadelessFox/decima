package com.shade.decima.rtti.factory;

import com.shade.util.NotNull;

public sealed interface TypeName extends Comparable<TypeName> {
    @NotNull
    static TypeName of(@NotNull String name) {
        return new Simple(name);
    }

    @NotNull
    static TypeName of(@NotNull String name, TypeName argument) {
        return new Parameterized(name, argument);
    }

    @NotNull
    static TypeName parse(@NotNull String name) {
        int start = name.indexOf('<');
        if (start < 0) {
            return of(name);
        }
        int end = name.lastIndexOf('>');
        if (start == 0 || end < start + 1) {
            throw new IllegalArgumentException("Invalid parameterized name: '" + name + "'");
        }
        String rawType = name.substring(0, start);
        String argumentType = name.substring(start + 1, end);
        return of(rawType, parse(argumentType));
    }

    @NotNull
    String fullName();

    @Override
    default int compareTo(TypeName o) {
        return fullName().compareTo(o.fullName());
    }

    record Simple(@NotNull String name) implements TypeName {
        @NotNull
        @Override
        public String fullName() {
            return name;
        }

        @Override
        public String toString() {
            return fullName();
        }
    }

    record Parameterized(@NotNull String name, @NotNull TypeName argument) implements TypeName {
        @NotNull
        @Override
        public String fullName() {
            return name + '<' + argument.fullName() + '>';
        }

        @Override
        public String toString() {
            return fullName();
        }
    }
}
