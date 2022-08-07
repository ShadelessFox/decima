package com.shade.decima.ui.controls;

import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ColoredComponent extends JComponent {
    private final List<ColoredFragment> fragments = new ArrayList<>(3);

    private Icon icon;
    private Insets padding;
    private int iconTextGap;

    public ColoredComponent() {
        iconTextGap = UIScale.scale(4);
        padding = new Insets(1, 2, 1, 2);
        setOpaque(true);
        updateUI();
    }

    public void append(@NotNull String fragment, @NotNull TextAttributes attributes) {
        ColoredFragment lastFragment;
        if (fragments.isEmpty()) {
            lastFragment = null;
        } else {
            lastFragment = fragments.get(fragments.size() - 1);
        }
        if (lastFragment != null && lastFragment.attributes().equals(attributes)) {
            fragments.set(fragments.size() - 1, new ColoredFragment(lastFragment.text() + fragment, attributes));
        } else {
            fragments.add(new ColoredFragment(fragment, attributes));
        }
        repaint();
    }

    public void clear() {
        fragments.clear();
        icon = null;
        repaint();
    }

    @Nullable
    public Icon getIcon() {
        return icon;
    }

    public void setIcon(@Nullable Icon icon) {
        if (Objects.equals(this.icon, icon)) {
            return;
        }
        this.icon = icon;
        repaint();
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        if (iconTextGap < 0) {
            throw new IllegalArgumentException("iconTextGap < 0: " + iconTextGap);
        }
        if (this.iconTextGap == iconTextGap) {
            return;
        }
        this.iconTextGap = iconTextGap;
        repaint();
    }

    @NotNull
    public Insets getPadding() {
        return padding;
    }

    public void setPadding(@NotNull Insets padding) {
        if (this.padding.equals(padding)) {
            return;
        }
        this.padding = padding;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) Math.ceil(computePreferredWidth()), computePreferredHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        doPaint((Graphics2D) g);
    }

    private void doPaint(@NotNull Graphics2D g) {
        int offset = 0;

        if (icon != null) {
            offset += padding.left;
            doPaintIcon(g, icon, offset);
            offset += icon.getIconWidth() + iconTextGap;
        }

        doPaintTextBackground(g, offset);
        doPaintTextFragments(g, offset);
    }

    private void doPaintIcon(@NotNull Graphics2D g, @NotNull Icon icon, int offset) {
        final Rectangle area = computePaintArea();
        final int y = area.y + (area.height - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g, offset, y);
    }

    private void doPaintTextFragments(@NotNull Graphics2D g, int startOffset) {
        UIUtils.setRenderingHints(g);

        float offset = startOffset;
        boolean wasSmaller = false;

        if (icon == null) {
            offset += padding.left;
        }

        for (ColoredFragment fragment : fragments) {
            final TextAttributes attributes = fragment.attributes();
            final Font font = deriveFontFromAttributes(getFont(), attributes, wasSmaller);
            final FontMetrics metrics = getFontMetrics(font);

            final Rectangle area = computePaintArea();
            final int fragmentBaseline = area.y + (area.height - metrics.getHeight() + 1) / 2 + metrics.getAscent();
            final float fragmentWidth = computeFragmentWidth(fragment, font);

            g.setFont(font);
            g.setColor(Objects.requireNonNullElseGet(attributes.foreground(), this::getForeground));
            g.drawString(fragment.text, offset, fragmentBaseline);

            offset = offset + fragmentWidth;
            wasSmaller = attributes.isSmaller();
        }
    }

    private void doPaintTextBackground(@NotNull Graphics2D g, int offset) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(offset, 0, getWidth() - offset, getHeight());
        }
    }

    @SuppressWarnings("MagicConstant")
    @NotNull
    private Font deriveFontFromAttributes(@NotNull Font font, @NotNull TextAttributes attributes, boolean wasSmaller) {
        final int style = attributes.fontStyle();

        if (wasSmaller != attributes.isSmaller()) {
            return font.deriveFont(style, UIUtils.getSmallerFontSize());
        }

        if (font.getStyle() != style) {
            return font.deriveFont(style);
        }

        return font;
    }

    @NotNull
    private Rectangle computePaintArea() {
        final Rectangle area = new Rectangle(getWidth(), getHeight());
        UIUtils.removeFrom(area, getInsets());
        UIUtils.removeFrom(area, padding);
        return area;
    }

    private float computeFragmentWidth(@NotNull ColoredFragment fragment, @NotNull Font font) {
        final FontMetrics metrics = getFontMetrics(font);
        final Rectangle2D bounds = font.getStringBounds(fragment.text(), metrics.getFontRenderContext());
        return (float) bounds.getWidth();
    }

    private float computePreferredWidth() {
        final Insets insets = getInsets();

        int width = 0;
        boolean wasSmaller = false;

        width += padding.left + insets.left;
        width += padding.right + insets.right;

        for (ColoredFragment fragment : fragments) {
            final TextAttributes attributes = fragment.attributes();
            final Font font = deriveFontFromAttributes(getFont(), attributes, wasSmaller);
            width += computeFragmentWidth(fragment, font);
            wasSmaller = attributes.isSmaller();
        }

        if (icon != null) {
            width += icon.getIconWidth() + iconTextGap;
        }

        return width;
    }

    private int computePreferredHeight() {
        final Font font = getFont();
        final FontMetrics metrics = getFontMetrics(font);
        final Insets insets = getInsets();

        int height = Math.min(UIScale.scale(16), metrics.getHeight());

        if (icon != null) {
            height = Math.max(height, icon.getIconHeight());
        }

        height += padding.top + insets.top;
        height += padding.bottom + insets.bottom;

        return height;
    }

    private static record ColoredFragment(@NotNull String text, @NotNull TextAttributes attributes) {}
}
