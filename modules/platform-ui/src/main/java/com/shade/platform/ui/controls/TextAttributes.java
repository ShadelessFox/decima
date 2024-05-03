package com.shade.platform.ui.controls;

import com.shade.platform.ui.UIColor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public record TextAttributes(@Nullable Color foreground, @Nullable Color background, @NotNull EnumSet<Style> styles) {
    public static final TextAttributes REGULAR_ATTRIBUTES = new TextAttributes(null, Style.PLAIN);
    public static final TextAttributes REGULAR_BOLD_ATTRIBUTES = REGULAR_ATTRIBUTES.bold();
    public static final TextAttributes REGULAR_ITALIC_ATTRIBUTES = REGULAR_ATTRIBUTES.italic();
    public static final TextAttributes REGULAR_MATCH_ATTRIBUTES = REGULAR_ATTRIBUTES.match();

    public static final TextAttributes GRAYED_ATTRIBUTES = new TextAttributes(UIColor.named("Label.disabledForeground"), Style.PLAIN);
    public static final TextAttributes GRAYED_BOLD_ATTRIBUTES = GRAYED_ATTRIBUTES.bold();
    public static final TextAttributes GRAYED_ITALIC_ATTRIBUTES = GRAYED_ATTRIBUTES.italic();
    public static final TextAttributes GRAYED_SMALL_ATTRIBUTES = GRAYED_ATTRIBUTES.smaller();

    public static final TextAttributes LINK_ATTRIBUTES = new TextAttributes(UIColor.named("Component.linkColor"), EnumSet.of(Style.PLAIN));

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

    @NotNull
    public TextAttributes match() {
        return alter(Style.MATCH);
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

    public boolean isMatch() {
        return styles.contains(Style.MATCH);
    }

    public enum Style {
        PLAIN,
        BOLD,
        ITALIC,
        SMALLER,
        MATCH
    }
}
