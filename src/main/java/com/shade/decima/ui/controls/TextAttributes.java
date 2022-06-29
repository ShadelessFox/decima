package com.shade.decima.ui.controls;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;

import java.awt.*;
import java.util.EnumSet;

public record TextAttributes(@Nullable Color foreground, @Nullable Color background, @NotNull EnumSet<Style> styles) {
    public static final TextAttributes REGULAR_ATTRIBUTES = new TextAttributes(null, Style.PLAIN);
    public static final TextAttributes REGULAR_BOLD_ATTRIBUTES = new TextAttributes(null, Style.BOLD);
    public static final TextAttributes REGULAR_ITALIC_ATTRIBUTES = new TextAttributes(null, Style.ITALIC);

    public static final TextAttributes GRAYED_ATTRIBUTES = new TextAttributes(UIUtils.getInactiveTextColor(), Style.PLAIN);
    public static final TextAttributes GRAYED_BOLD_ATTRIBUTES = new TextAttributes(UIUtils.getInactiveTextColor(), Style.BOLD);
    public static final TextAttributes GRAYED_ITALIC_ATTRIBUTES = new TextAttributes(UIUtils.getInactiveTextColor(), Style.ITALIC);
    public static final TextAttributes GRAYED_SMALL_ATTRIBUTES = new TextAttributes(UIUtils.getInactiveTextColor(), Style.SMALLER);

    public static final TextAttributes DARK_RED_ATTRIBUTES = new TextAttributes(new Color(0x800000), Style.PLAIN);
    public static final TextAttributes BLUE_ATTRIBUTES = new TextAttributes(new Color(0x0000FF), Style.PLAIN);

    public TextAttributes(@Nullable Color foregroundColor, @Nullable Color backgroundColor, @NotNull Style style, @NotNull Style... rest) {
        this(foregroundColor, backgroundColor, EnumSet.of(style, rest));
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull EnumSet<Style> styles) {
        this(foregroundColor, null, styles);
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull Style style, @NotNull Style... rest) {
        this(foregroundColor, null, style, rest);
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
