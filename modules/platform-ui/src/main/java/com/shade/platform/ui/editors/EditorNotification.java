package com.shade.platform.ui.editors;

import com.shade.platform.ui.UIColor;
import com.shade.util.NotNull;

import java.awt.*;
import java.util.Collection;

public record EditorNotification(@NotNull Status status, @NotNull String message, @NotNull Collection<Action> actions) {
    public enum Status {
        ERROR(UIColor.named("Component.error.background"), UIColor.named("Component.error.borderColor")),
        WARNING(UIColor.named("Component.warning.background"), UIColor.named("Component.warning.borderColor"));

        private final Color background;
        private final Color border;

        Status(@NotNull Color background, @NotNull Color border) {
            this.background = background;
            this.border = border;
        }

        @NotNull
        public Color getBackground() {
            return background;
        }

        @NotNull
        public Color getBorder() {
            return border;
        }
    }

    public record Action(@NotNull String name, @NotNull Runnable callback) {}
}
