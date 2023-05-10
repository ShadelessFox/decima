package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public enum DMFComponentType {
    SIGNED_SHORT_NORMALIZED("SignedShortNormalized", 2),
    FLOAT("Float", 4),
    HALF_FLOAT("HalfFloat", 2),
    UNSIGNED_BYTE_NORMALIZED("UnsignedByteNormalized", 1),
    SIGNED_SHORT("SignedShort", 2),
    UNSIGNED_SHORT("UnsignedShort", 2),
    UNSIGNED_SHORT_NORMALIZED("UnsignedShortNormalized", 2),
    X10Y10Z10W2NORMALIZED("X10Y10Z10W2Normalized", 4),
    UNSIGNED_BYTE("UnsignedByte", 1);

    private final String typeName;
    private final int size;

    DMFComponentType(String typeName, int size) {
        this.typeName = typeName;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    @NotNull
    public String getTypeName() {
        return typeName;
    }

    @NotNull
    public static DMFComponentType fromString(@NotNull String text) {
        for (DMFComponentType b : DMFComponentType.values()) {
            if (b.typeName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new EnumConstantNotPresentException(DMFComponentType.class, text);
    }
}
