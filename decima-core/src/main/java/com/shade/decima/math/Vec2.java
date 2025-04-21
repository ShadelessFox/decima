package com.shade.decima.math;

public record Vec2(float x, float y) {
    public Vec2 mul(float scalar) {
        return mul(scalar, scalar);
    }

    public Vec2 mul(float x, float y) {
        return new Vec2(this.x * x, this.y * y);
    }
}
