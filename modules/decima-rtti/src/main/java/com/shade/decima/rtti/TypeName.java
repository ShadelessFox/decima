package com.shade.decima.rtti;

import com.shade.util.NotNull;

public sealed interface TypeName {
    @NotNull
    static TypeName of(@NotNull String name) {
        int start = name.indexOf('<');
        if (start < 0) {
            return new Simple(name);
        }
        int end = name.lastIndexOf('>');
        if (start == 0 || end < start + 1) {
            throw new IllegalArgumentException("Invalid template name: '" + name + "'");
        }
        String rawType = name.substring(0, start);
        String argumentType = name.substring(start + 1, end);
        return new Parameterized(rawType, of(argumentType));
    }

    @NotNull
    String name();

    @NotNull
    String fullName();

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
