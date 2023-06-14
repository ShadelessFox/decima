package com.shade.platform.ui.controls.plaf;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatToolTipUI;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

public class FlatOutlineToolTipUI extends FlatToolTipUI {
    private Color borderColor;
    private Color errorBorderColor;
    private Color errorBackground;
    private Color warningBorderColor;
    private Color warningBackground;

    public static ComponentUI createUI(JComponent c) {
        return new FlatOutlineToolTipUI();
    }

    @Override
    protected void installDefaults(JComponent c) {
        super.installDefaults(c);

        borderColor = UIManager.getColor("Component.borderColor");
        errorBorderColor = UIManager.getColor("Component.error.focusedBorderColor");
        errorBackground = UIManager.getColor("Component.error.background");
        warningBorderColor = UIManager.getColor("Component.warning.focusedBorderColor");
        warningBackground = UIManager.getColor("Component.warning.background");
    }

    @Override
    protected void uninstallDefaults(JComponent c) {
        super.uninstallDefaults(c);

        borderColor = null;
        errorBorderColor = null;
        errorBackground = null;
        warningBorderColor = null;
        warningBackground = null;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        g.setColor(getColor(c, false));
        g.fillRect(0, 0, c.getWidth(), c.getHeight());

        g.setColor(getColor(c, true));
        g.drawRect(0, 0, c.getWidth() - 1, c.getHeight() - 1);

        super.paint(g, c);
    }

    @NotNull
    private Color getColor(@NotNull JComponent c, boolean border) {
        final Object outline = c.getClientProperty(FlatClientProperties.OUTLINE);

        if (outline instanceof String) {
            switch ((String) outline) {
                case FlatClientProperties.OUTLINE_ERROR:
                    return border ? errorBorderColor : errorBackground;
                case FlatClientProperties.OUTLINE_WARNING:
                    return border ? warningBorderColor : warningBackground;
                default:
                    break;
            }
        }

        return border ? borderColor : c.getBackground();
    }
}
