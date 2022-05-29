package com.shade.decima.ui.controls.validation;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public record Validation(@NotNull Type type, @Nullable String message) {
    @NotNull
    public static Validation ok() {
        return new Validation(Type.OK, null);
    }

    @NotNull
    public static Validation warning(@NotNull String message) {
        return new Validation(Type.WARNING, message);
    }

    @NotNull
    public static Validation error(@NotNull String message) {
        return new Validation(Type.ERROR, message);
    }

    public boolean isOK() {
        return type == Type.OK;
    }

    public enum Type {
        OK(null),
        WARNING(FlatClientProperties.OUTLINE_WARNING),
        ERROR(FlatClientProperties.OUTLINE_ERROR);

        private final String outline;

        Type(@Nullable String outline) {
            this.outline = outline;
        }

        @Nullable
        public String getOutline() {
            return outline;
        }
    }
}
