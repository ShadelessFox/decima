package com.shade.decima.game.hfw.data.jolt.physics.constraints;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class SwingTwistConstraintSettings extends TwoBodyConstraintSettings {
    public Vec3 position1;
    public Vec3 twistAxis1;
    public Vec3 planeAxis1;
    public Vec3 position2;
    public Vec3 twistAxis2;
    public Vec3 planeAxis2;
    public float normalHalfConeAngle;
    public float planeHalfConeAngle;
    public float twistMinAngle;
    public float twistMaxAngle;
    public float maxFrictionTorque;
    public final MotorSettings swingMotorSettings = new MotorSettings();
    public final MotorSettings twistMotorSettings = new MotorSettings();

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        position1 = JoltUtils.readAlignedVector3(reader);
        twistAxis1 = JoltUtils.readAlignedVector3(reader);
        planeAxis1 = JoltUtils.readAlignedVector3(reader);
        position2 = JoltUtils.readAlignedVector3(reader);
        twistAxis2 = JoltUtils.readAlignedVector3(reader);
        planeAxis2 = JoltUtils.readAlignedVector3(reader);
        normalHalfConeAngle = reader.readFloat();
        planeHalfConeAngle = reader.readFloat();
        twistMinAngle = reader.readFloat();
        twistMaxAngle = reader.readFloat();
        maxFrictionTorque = reader.readFloat();
        swingMotorSettings.restoreBinaryState(reader);
        twistMotorSettings.restoreBinaryState(reader);
    }
}
