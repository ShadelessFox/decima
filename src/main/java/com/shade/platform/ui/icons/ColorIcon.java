package com.shade.platform.ui.icons;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class ColorIcon implements Icon {
    private final Supplier<Color> supplier;

    public ColorIcon(@NotNull Supplier<Color> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Color color = supplier.get();

        g.setColor(UIManager.getColor("Actions.Grey"));
        g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);

        g.setColor(Color.WHITE);
        g.drawRect(x + 1, y + 1, getIconWidth() - 3, getIconHeight() - 3);

        for (int x0 = x; x0 < x + 12; x0 += 3) {
            for (int y0 = y; y0 < y + 12; y0 += 3) {
                final int i = (x0 + y0 % 6) % 6;
                g.setColor(getColor(color, i <= 0));
                g.fillRect(x0 + 2, y0 + 2, 3, 3);
            }
        }
    }

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }

    @NotNull
    public static Color getColor(@NotNull Color color, boolean odd) {
        return switch (color.getTransparency()) {
            case Transparency.OPAQUE -> color;
            case Transparency.TRANSLUCENT -> lerp(odd ? Color.WHITE : Color.LIGHT_GRAY, color, color.getAlpha() / 255.0f);
            default -> odd ? Color.WHITE : Color.LIGHT_GRAY;
        };
    }

    @NotNull
    private static Color lerp(@NotNull Color a, @NotNull Color b, float t) {
        return new Color(
            (int) (a.getRed() + t * (b.getRed() - a.getRed())),
            (int) (a.getGreen() + t * (b.getGreen() - a.getGreen())),
            (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()))
        );
    }
}
