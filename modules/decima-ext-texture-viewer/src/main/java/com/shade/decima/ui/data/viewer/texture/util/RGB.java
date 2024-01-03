package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

public record RGB(int argb) {
    public static final RGB TRANSPARENT = new RGB(0, 0, 0, 0);

    public RGB(int r, int g, int b, int a) {
        this((a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | b & 0xff);
    }

    public RGB(int r, int g, int b) {
        this(r, g, b, 0xff);
    }

    @NotNull
    public static RGB from565(int value) {
        final int r = (value >>> 11 & 31) * 255 / 31;
        final int g = (value >>> 5 & 63) * 255 / 63;
        final int b = (value & 31) * 255 / 31;
        return new RGB(r, g, b);
    }

    @NotNull
    public static RGB mix(@NotNull RGB first, @NotNull RGB second, float factor) {
        final float inv = 1f - factor;
        final int r = (int) (first.r() * factor + second.r() * inv);
        final int g = (int) (first.g() * factor + second.g() * inv);
        final int b = (int) (first.b() * factor + second.b() * inv);
        return new RGB(r, g, b);
    }

    public static int mix(int first, int second, float factor) {
        return (int) (first * factor + second * (1f - factor));
    }

    public int a() {
        return argb >> 24 & 0xff;
    }

    public int r() {
        return argb >> 16 & 0xff;
    }

    public int g() {
        return argb >> 8 & 0xff;
    }

    public int b() {
        return argb & 0xff;
    }

    @NotNull
    public RGB map(@NotNull PixelMapper mapper, boolean withAlpha) {
        return new RGB(
            mapper.apply(r()),
            mapper.apply(g()),
            mapper.apply(b()),
            withAlpha ? mapper.apply(a()) : a()
        );
    }

    @NotNull
    public RGB map(@NotNull IndexedPixelMapper mapper, boolean withAlpha) {
        return new RGB(
            mapper.apply(0, r()),
            mapper.apply(1, g()),
            mapper.apply(2, b()),
            withAlpha ? mapper.apply(3, a()) : a()
        );
    }

    @NotNull
    public RGB r(int r) {
        if (r == r()) {
            return this;
        }
        return new RGB(r, g(), b(), a());
    }

    @NotNull
    public RGB g(int g) {
        if (g == g()) {
            return this;
        }
        return new RGB(r(), g, b(), a());
    }

    @NotNull
    public RGB b(int b) {
        if (b == b()) {
            return this;
        }
        return new RGB(r(), g(), b, a());
    }

    @NotNull
    public RGB a(int a) {
        if (a == a()) {
            return this;
        }
        return new RGB(r(), g(), b(), a);
    }

    @Override
    public String toString() {
        return "RGB[r=" + r() + " g=" + g() + " b=" + b() + " a=" + a() + "]";
    }

    public interface PixelMapper {
        int apply(int value);
    }

    public interface IndexedPixelMapper {
        int apply(int index, int value);
    }
}
