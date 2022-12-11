package com.shade.platform.ui.controls;

import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public record TextAttributes(@Nullable Color foreground, @Nullable Color background, @NotNull EnumSet<Style> styles) {
    public static final TextAttributes REGULAR_ATTRIBUTES = new TextAttributes(null, Style.PLAIN);
    public static final TextAttributes REGULAR_BOLD_ATTRIBUTES = REGULAR_ATTRIBUTES.bold();
    public static final TextAttributes REGULAR_ITALIC_ATTRIBUTES = REGULAR_ATTRIBUTES.italic();

    public static final TextAttributes GRAYED_ATTRIBUTES = new TextAttributes(UIUtils.getInactiveTextColor(), Style.PLAIN);
    public static final TextAttributes GRAYED_BOLD_ATTRIBUTES = GRAYED_ATTRIBUTES.bold();
    public static final TextAttributes GRAYED_ITALIC_ATTRIBUTES = GRAYED_ATTRIBUTES.italic();
    public static final TextAttributes GRAYED_SMALL_ATTRIBUTES = GRAYED_ATTRIBUTES.smaller();

    public TextAttributes(@Nullable Color foregroundColor, @Nullable Color backgroundColor, @NotNull Style style, @NotNull Style... rest) {
        this(foregroundColor, backgroundColor, EnumSet.of(style, rest));
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull EnumSet<Style> styles) {
        this(foregroundColor, null, styles);
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull Style style, @NotNull Style... rest) {
        this(foregroundColor, null, style, rest);
    }

    @NotNull
    public TextAttributes alter(@NotNull Style style, @NotNull Style... rest) {
        final EnumSet<Style> styles = this.styles.clone();

        if (styles.add(style) | styles.addAll(List.of(rest))) {
            return new TextAttributes(foreground, background, styles);
        } else {
            return this;
        }
    }

    @NotNull
    public TextAttributes smaller() {
        return alter(Style.SMALLER);
    }

    @NotNull
    public TextAttributes bold() {
        return alter(Style.BOLD);
    }

    @NotNull
    public TextAttributes italic() {
        return alter(Style.ITALIC);
    }

    public int fontStyle() {
        int style = Font.PLAIN;
        if (styles.contains(Style.BOLD)) {
            style |= Font.BOLD;
        }
        if (styles.contains(Style.ITALIC)) {
            style |= Font.ITALIC;
        }
        return style;
    }

    public boolean isSmaller() {
        return styles.contains(Style.SMALLER);
    }

    public enum Style {
        PLAIN,
        BOLD,
        ITALIC,
        SMALLER
    }
}
