package com.shade.platform.ui;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.function.Supplier;

public class UIColor extends Color {
    public static final UIColor SHADOW = named("Separator.shadow");

    private final String name;
    private final Supplier<? extends Color> supplier;

    public UIColor(int rgb) {
        super(rgb, (rgb & 0xFF000000) != 0);
        this.name = null;
        this.supplier = null;
    }

    public UIColor(@NotNull Color color) {
        super(color.getRGB(), color.getAlpha() != 255);
        this.name = null;
        this.supplier = null;
    }

    public UIColor(@NotNull String name) {
        super(0);
        this.name = name;
        this.supplier = null;
    }

    public UIColor(@NotNull Supplier<? extends Color> supplier) {
        super(0);
        this.name = null;
        this.supplier = supplier;
    }

    @NotNull
    public static UIColor named(@NotNull String name) {
        return new UIColor(name);
    }

    @Override
    public int getRed() {
        final Color c = getColor();
        return c == this ? super.getRed() : c.getRed();
    }

    @Override
    public int getGreen() {
        final Color c = getColor();
        return c == this ? super.getGreen() : c.getGreen();
    }

    @Override
    public int getBlue() {
        final Color c = getColor();
        return c == this ? super.getBlue() : c.getBlue();
    }

    @Override
    public int getAlpha() {
        final Color c = getColor();
        return c == this ? super.getAlpha() : c.getAlpha();
    }

    @Override
    public int getRGB() {
        final Color c = getColor();
        return c == this ? super.getRGB() : c.getRGB();
    }

    @NotNull
    @Override
    public Color brighter() {
        if (supplier != null) {
            return new UIColor(() -> supplier.get().brighter());
        }
        if (name != null) {
            return UIManager.getColor(name).brighter();
        }
        return new UIColor(super.brighter());
    }

    @NotNull
    @Override
    public Color darker() {
        if (supplier != null) {
            return new UIColor(() -> supplier.get().darker());
        }
        if (name != null) {
            return UIManager.getColor(name).darker();
        }
        return new UIColor(super.darker());
    }

    @NotNull
    @Override
    public float[] getRGBComponents(float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getRGBComponents(compArray) : c.getRGBComponents(compArray);
    }

    @NotNull
    @Override
    public float[] getRGBColorComponents(float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getRGBComponents(compArray) : c.getRGBColorComponents(compArray);
    }

    @NotNull
    @Override
    public float[] getComponents(float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getComponents(compArray) : c.getComponents(compArray);
    }

    @NotNull
    @Override
    public float[] getColorComponents(float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getColorComponents(compArray) : c.getColorComponents(compArray);
    }

    @NotNull
    @Override
    public float[] getComponents(@NotNull ColorSpace colorSpace, float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getComponents(colorSpace, compArray) : c.getComponents(colorSpace, compArray);
    }

    @NotNull
    @Override
    public float[] getColorComponents(@NotNull ColorSpace colorSpace, float[] compArray) {
        final Color c = getColor();
        return c == this ? super.getColorComponents(colorSpace, compArray) : c.getColorComponents(colorSpace, compArray);
    }

    @NotNull
    @Override
    public ColorSpace getColorSpace() {
        final Color c = getColor();
        return c == this ? super.getColorSpace() : c.getColorSpace();
    }

    @NotNull
    @Override
    public synchronized PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d, AffineTransform affineTransform, RenderingHints hints) {
        final Color c = getColor();
        return c == this ? super.createContext(cm, r, r2d, affineTransform, hints) : c.createContext(cm, r, r2d, affineTransform, hints);
    }

    @Override
    public int getTransparency() {
        final Color c = getColor();
        return c == this ? super.getTransparency() : c.getTransparency();
    }

    @Override
    public int hashCode() {
        final Color c = getColor();
        return c == this ? super.hashCode() : c.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        final Color c = getColor();
        return c == this ? super.equals(obj) : c.equals(obj);
    }

    @NotNull
    @Override
    public String toString() {
        final Color c = getColor();
        return c == this ? super.toString() : c.toString();
    }

    @NotNull
    private Color getColor() {
        if (supplier != null) {
            return supplier.get();
        }
        if (name != null) {
            return UIManager.getColor(name);
        }
        return this;
    }
}
