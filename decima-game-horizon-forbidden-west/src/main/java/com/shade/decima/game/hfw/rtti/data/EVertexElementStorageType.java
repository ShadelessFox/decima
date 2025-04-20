package com.shade.decima.game.hfw.rtti.data;

import com.shade.decima.rtti.Serializable;
import com.shade.decima.rtti.data.Value;

@Serializable(size = 1)
public enum EVertexElementStorageType implements Value.OfEnum<EVertexElementStorageType> {
    Undefined("Undefined", 0),
    SignedShortNormalized("SignedShortNormalized", 1),
    Float("Float", 2),
    HalfFloat("HalfFloat", 3),
    UnsignedByteNormalized("UnsignedByteNormalized", 4),
    SignedShort("SignedShort", 5),
    X10Y10Z10W2Normalized("X10Y10Z10W2Normalized", 6),
    UnsignedByte("UnsignedByte", 7),
    UnsignedShort("UnsignedShort", 8),
    UnsignedShortNormalized("UnsignedShortNormalized", 9),
    UNorm8sRGB("UNorm8sRGB", 10),
    X10Y10Z10W2UNorm("X10Y10Z10W2UNorm", 11);

    private final String name;
    private final int value;

    EVertexElementStorageType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static EVertexElementStorageType valueOf(int value) {
        return (EVertexElementStorageType) Value.valueOf(EVertexElementStorageType.class, value);
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
