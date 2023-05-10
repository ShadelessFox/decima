package com.shade.platform.ui.icons;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class OverlaidIcon extends FlatAbstractIcon {
    private static final Shape CLIP = createClip();

    private final Icon baseIcon;
    private final Icon overlayIcon;
    private FlatSVGIcon.ColorFilter colorFilter;

    public OverlaidIcon(@NotNull Icon baseIcon, @NotNull Icon overlayIcon) {
        super(16, 16, null);
        this.baseIcon = baseIcon;
        this.overlayIcon = overlayIcon;
    }

    public OverlaidIcon(@NotNull OverlaidIcon icon) {
        this(icon.baseIcon, icon.overlayIcon);
        this.colorFilter = null;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g2) {
        final Icon overlayIcon = UIUtils.applyColorFilter(this.overlayIcon, colorFilter);
        final Icon baseIcon = UIUtils.applyColorFilter(this.baseIcon, colorFilter);

        overlayIcon.paintIcon(c, g2, 8, 8);
        g2.setClip(CLIP);
        baseIcon.paintIcon(c, g2, 0, 0);
    }

    public void setColorFilter(@Nullable FlatSVGIcon.ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
    }

    @NotNull
    private static Path2D createClip() {
        final var ci = 1. - 0.5522847498307933;
        final var arc = 8 * ci;

        final Path2D clip = new Path2D.Float();
        clip.moveTo(0, 0);
        clip.lineTo(16, 0);
        clip.lineTo(18, 12);
        clip.curveTo(18, 12 - arc, 12 + arc, 6, 12, 6);
        clip.curveTo(12 - arc, 6, 6, 12 - arc, 6, 12);
        clip.curveTo(6, 12 + arc, 12 - arc, 18, 12, 18);
        clip.lineTo(0, 16);
        clip.closePath();

        return clip;
    }
}
