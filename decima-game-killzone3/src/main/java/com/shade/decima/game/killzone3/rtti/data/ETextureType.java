package com.shade.decima.game.killzone3.rtti.data;

import com.shade.decima.rtti.Serializable;
import com.shade.decima.rtti.data.Value;

@Serializable(size = 4)
public enum ETextureType implements Value.OfEnum<ETextureType> {
    _2D("2D", 0),
    _3D("3D", 1),
    CubeMap("CubeMap", 2);

    private final String name;
    private final int value;

    ETextureType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static ETextureType valueOf(int value) {
        return (ETextureType) Value.valueOf(ETextureType.class, value);
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
