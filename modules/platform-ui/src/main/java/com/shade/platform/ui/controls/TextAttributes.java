package com.shade.platform.ui.controls;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class TextAttributes {
    public static final TextAttributes REGULAR_ATTRIBUTES = new TextAttributes((Color) null, Style.PLAIN);
    public static final TextAttributes REGULAR_BOLD_ATTRIBUTES = REGULAR_ATTRIBUTES.bold();
    public static final TextAttributes REGULAR_ITALIC_ATTRIBUTES = REGULAR_ATTRIBUTES.italic();
    public static final TextAttributes REGULAR_MATCH_ATTRIBUTES = REGULAR_ATTRIBUTES.match();

    public static final TextAttributes GRAYED_ATTRIBUTES = new TextAttributes("Label.disabledForeground", Style.PLAIN);
    public static final TextAttributes GRAYED_BOLD_ATTRIBUTES = GRAYED_ATTRIBUTES.bold();
    public static final TextAttributes GRAYED_ITALIC_ATTRIBUTES = GRAYED_ATTRIBUTES.italic();
    public static final TextAttributes GRAYED_SMALL_ATTRIBUTES = GRAYED_ATTRIBUTES.smaller();

    private final ColorSource foreground;
    private final ColorSource background;
    private final EnumSet<Style> styles;

    public TextAttributes(@NotNull ColorSource foreground, @NotNull ColorSource background, @NotNull EnumSet<Style> styles) {
        this.foreground = foreground;
        this.background = background;
        this.styles = styles;
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull EnumSet<Style> styles) {
        this(
            foregroundColor != null ? new ColorSource.Direct(foregroundColor) : ColorSource.None.INSTANCE,
            ColorSource.None.INSTANCE,
            styles
        );
    }

    public TextAttributes(@Nullable Color foregroundColor, @NotNull Style style, @NotNull Style... rest) {
        this(
            foregroundColor != null ? new ColorSource.Direct(foregroundColor) : ColorSource.None.INSTANCE,
            ColorSource.None.INSTANCE,
            EnumSet.of(style, rest)
        );
    }

    public TextAttributes(@Nullable String foregroundKey, @NotNull Style style, @NotNull Style... rest) {
        this(
            foregroundKey != null ? new ColorSource.Key(foregroundKey) : ColorSource.None.INSTANCE,
            ColorSource.None.INSTANCE,
            EnumSet.of(style, rest)
        );
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

    @Nullable
    public Color foreground() {
        return foreground.get();
    }

    @Nullable
    public Color background() {
        return background.get();
    }

    @NotNull
    public EnumSet<Style> styles() {
        return styles;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TextAttributes) obj;
        return Objects.equals(this.foreground, that.foreground) &&
            Objects.equals(this.background, that.background) &&
            Objects.equals(this.styles, that.styles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background, styles);
    }

    @Override
    public String toString() {
        return "TextAttributes[" +
            "foreground=" + foreground + ", " +
            "background=" + background + ", " +
            "styles=" + styles + ']';
    }

    public enum Style {
        PLAIN,
        BOLD,
        ITALIC,
        SMALLER,
        MATCH
    }

    public sealed interface ColorSource {
        @Nullable
        Color get();

        record Direct(@NotNull Color color) implements ColorSource {
            @NotNull
            @Override
            public Color get() {
                return color;
            }
        }

        record Key(@NotNull String key) implements ColorSource {
            @NotNull
            @Override
            public Color get() {
                return UIManager.getColor(key);
            }
        }

        enum None implements ColorSource {
            INSTANCE;

            @Override
            public Color get() {
                return null;
            }
        }
    }
}
