package com.shade.platform.ui.controls;

import com.formdev.flatlaf.util.UIScale;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ColoredComponent extends JComponent {
    private static final boolean DEBUG_OVERLAY = false;

    private final List<ColoredFragment> fragments = new ArrayList<>(3);

    private Icon leadingIcon;
    private Icon trailingIcon;
    private Insets padding;
    private int iconTextGap;

    public ColoredComponent() {
        iconTextGap = UIScale.scale(4);
        padding = new Insets(1, 2, 1, 2);
        setOpaque(true);
        updateUI();
    }

    public void append(@NotNull String fragment, @NotNull TextAttributes attributes) {
        synchronized (fragments) {
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
        }

        repaint();
    }

    public void clear() {
        synchronized (fragments) {
            fragments.clear();
            leadingIcon = null;
            trailingIcon = null;
        }

        repaint();
    }

    @Nullable
    public Icon getLeadingIcon() {
        return leadingIcon;
    }

    public void setLeadingIcon(@Nullable Icon leadingIcon) {
        if (Objects.equals(this.leadingIcon, leadingIcon)) {
            return;
        }
        this.leadingIcon = leadingIcon;
        repaint();
    }

    @Nullable
    public Icon getTrailingIcon() {
        return trailingIcon;
    }

    public void setTrailingIcon(@Nullable Icon trailingIcon) {
        if (Objects.equals(this.trailingIcon, trailingIcon)) {
            return;
        }
        this.trailingIcon = trailingIcon;
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
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension((int) Math.ceil(computePreferredWidth()), computePreferredHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        doPaint((Graphics2D) g);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        synchronized (fragments) {
            for (ColoredFragment fragment : fragments) {
                sb.append(fragment.text);
            }
        }
        return sb.toString();
    }

    private void doPaint(@NotNull Graphics2D g) {
        int offset = 0;

        if (leadingIcon != null) {
            doPaintIconBackground(g, leadingIcon, offset, padding.left);
            offset += padding.left;
            doPaintIcon(g, leadingIcon, offset);
            offset += leadingIcon.getIconWidth() + iconTextGap;
        }

        doPaintTextBackground(g, offset);
        offset += doPaintTextFragments(g, offset);

        if (trailingIcon != null) {
            offset += iconTextGap;
            doPaintIconBackground(g, trailingIcon, offset, padding.right);
            doPaintIcon(g, trailingIcon, offset);
        }
    }

    private void doPaintIcon(@NotNull Graphics2D g, @NotNull Icon icon, int offset) {
        final Rectangle area = computePaintArea();
        final int y = area.y + (area.height - icon.getIconHeight()) / 2;

        if (DEBUG_OVERLAY) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(offset, y, icon.getIconWidth() - 1, icon.getIconHeight() - 1);
        }

        icon.paintIcon(this, g, offset, y);
    }

    private void doPaintIconBackground(@NotNull Graphics2D g, @NotNull Icon icon, int offset, int padding) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(offset, 0, padding + icon.getIconWidth() + iconTextGap, getHeight());
        }
    }

    private int doPaintTextFragments(@NotNull Graphics2D g, int startOffset) {
        UIUtils.setRenderingHints(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float offset = startOffset;
        boolean wasSmaller = false;

        if (leadingIcon == null) {
            offset += padding.left;
        }

        final Rectangle area = computePaintArea();
        final Font baseFont = getBaseFont();

        synchronized (fragments) {
            for (ColoredFragment fragment : fragments) {
                final TextAttributes attributes = fragment.attributes();
                final Font font = deriveFontFromAttributes(baseFont, attributes, wasSmaller);
                final FontMetrics metrics = getFontMetrics(font);

                final int fragmentBaseline = area.y + area.height - metrics.getDescent();
                final float fragmentWidth = computeFragmentWidth(fragment, font);
                final Color color;

                if (attributes.isMatch()) {
                    final Composite composite = g.getComposite();

                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                    g.setColor(UIManager.getColor("ColoredComponent.matchBackground"));
                    g.fillRoundRect((int) offset - 1, area.y, (int) fragmentWidth + 2, area.height, 4, 4);
                    g.drawRoundRect((int) offset - 1, area.y, (int) fragmentWidth + 1, area.height - 1, 4, 4);
                    g.setComposite(composite);

                    color = UIManager.getColor("ColoredComponent.matchForeground");
                } else {
                    color = Objects.requireNonNullElseGet(attributes.foreground(), this::getForeground);
                }

                if (DEBUG_OVERLAY) {
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawRect((int) offset, area.y, (int) fragmentWidth - 1, area.height - 1);
                }

                g.setFont(font);
                g.setColor(color);
                g.drawString(fragment.text, offset, fragmentBaseline);

                offset = offset + fragmentWidth;
                wasSmaller = attributes.isSmaller();
            }
        }

        return (int) offset - startOffset;
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
        final Rectangle area = new Rectangle(getPreferredSize());
        area.x += (getWidth() - area.width) / 2;
        area.y += (getHeight() - area.height) / 2;

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
        final Font baseFont = getBaseFont();

        float width = 0;
        boolean wasSmaller = false;

        width += padding.left + insets.left;
        width += padding.right + insets.right;

        synchronized (fragments) {
            for (ColoredFragment fragment : fragments) {
                final TextAttributes attributes = fragment.attributes();
                final Font font = deriveFontFromAttributes(baseFont, attributes, wasSmaller);
                width += computeFragmentWidth(fragment, font);
                wasSmaller = attributes.isSmaller();
            }
        }

        if (leadingIcon != null) {
            width += leadingIcon.getIconWidth() + iconTextGap;
        }

        if (trailingIcon != null) {
            width += trailingIcon.getIconWidth() + iconTextGap;
        }

        return width;
    }

    private int computePreferredHeight() {
        final Font font = getBaseFont();
        final FontMetrics metrics = getFontMetrics(font);
        final Insets insets = getInsets();

        int height = Math.max(UIScale.scale(16), metrics.getHeight());

        if (leadingIcon != null) {
            height = Math.max(height, leadingIcon.getIconHeight());
        }

        if (trailingIcon != null) {
            height = Math.max(height, trailingIcon.getIconHeight());
        }

        height += padding.top + insets.top;
        height += padding.bottom + insets.bottom;

        return height;
    }

    @NotNull
    private Font getBaseFont() {
        Font font = getFont();

        if (font == null) {
            font = UIUtils.getDefaultFont();
        }

        return font;
    }

    private record ColoredFragment(@NotNull String text, @NotNull TextAttributes attributes) {}
}
