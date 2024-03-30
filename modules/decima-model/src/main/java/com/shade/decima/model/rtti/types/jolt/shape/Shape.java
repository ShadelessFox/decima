package com.shade.decima.model.rtti.types.jolt.shape;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class Shape {
    protected int mUserData;

    @NotNull
    public static Shape restoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final ShapeSubType shapeSubType = ShapeSubType.values()[buffer.get()];
        final Shape shape = ShapeFunctions.get(shapeSubType).construct();
        shape.restoreBinaryState(buffer);
        return shape;
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        mUserData = buffer.getInt();
    }
}
