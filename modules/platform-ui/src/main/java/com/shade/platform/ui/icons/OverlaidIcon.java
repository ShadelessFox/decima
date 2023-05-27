package com.shade.platform.ui.icons;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatAbstractIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

public class OverlaidIcon extends FlatAbstractIcon {
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
    protected void paintIcon(Component c, Graphics2D g) {
        final Icon overlayIcon = UIUtils.applyColorFilter(this.overlayIcon, colorFilter);
        final Icon baseIcon = UIUtils.applyColorFilter(this.baseIcon, colorFilter);

        baseIcon.paintIcon(c, g, 0, 0);
        g.fillOval(7, 7, 10, 10);
        overlayIcon.paintIcon(c, g, 8, 8);
    }

    public void setColorFilter(@Nullable FlatSVGIcon.ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
    }
}
