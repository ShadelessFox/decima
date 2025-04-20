package com.shade.platform.ui.icons;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class OverlaidIcon extends FlatAbstractIcon {
    private final Icon baseIcon;
    private final Icon overlayIcon;

    public OverlaidIcon(@NotNull Icon baseIcon, @NotNull Icon overlayIcon) {
        super(16, 16, null);
        this.baseIcon = baseIcon;
        this.overlayIcon = overlayIcon;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        baseIcon.paintIcon(c, g, 0, 0);
        g.fillOval(7, 7, 10, 10);
        overlayIcon.paintIcon(c, g, 8, 8);
    }
}
