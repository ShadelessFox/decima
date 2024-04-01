package com.shade.decima.model.rtti.types.jolt.physics.constraints;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class PointConstraintSettings extends TwoBodyConstraintSettings {

    Vector3f commonPoint = new Vector3f(0.0f);


    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        commonPoint = JoltUtils.getAlignedVector3(buffer);
    }
}
