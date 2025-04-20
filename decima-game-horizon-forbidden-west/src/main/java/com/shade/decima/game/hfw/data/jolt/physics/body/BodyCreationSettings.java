package com.shade.decima.game.hfw.data.jolt.physics.body;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.decima.game.hfw.data.jolt.math.Quat;
import com.shade.decima.game.hfw.data.jolt.math.Vec3;
import com.shade.decima.game.hfw.data.jolt.physics.collision.CollisionGroup;
import com.shade.decima.game.hfw.data.jolt.physics.collision.GroupFilter;
import com.shade.decima.game.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.decima.game.hfw.data.jolt.physics.collision.shape.Shape;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class BodyCreationSettings {
    public enum OverrideMassProperties {
        CalculateMassAndInertia,
        CalculateInertia,
        MassAndInertiaProvided
    }

    public Vec3 position;
    public Quat rotation;
    public CollisionGroup collisionGroup;
    public short objectLayer;
    public MotionType motionType;
    public boolean allowDynamicOrKinematic;
    public MotionQuality motionQuality;
    public boolean allowSleeping;
    public float friction;
    public float restitution;
    public float linearDamping;
    public float angularDamping;
    public float maxLinearVelocity;
    public float maxAngularVelocity;
    public float gravityFactor;
    public OverrideMassProperties overrideMassProperties;
    public float inertiaMultiplier;
    public MassProperties massPropertiesOverride;
    public Shape shape;

    @NotNull
    public static BodyCreationSettings sRestoreWithChildren(
        @NotNull BinaryReader reader,
        @NotNull List<Shape> shapeMap,
        @NotNull List<PhysicsMaterial> materialMap,
        @NotNull List<GroupFilter> groupFilterMap
    ) throws IOException {
        BodyCreationSettings settings = new BodyCreationSettings();
        settings.restoreBinaryState(reader);
        settings.shape = Shape.sRestoreWithChildren(reader, shapeMap, materialMap);

        int groupFilterId = reader.readInt();
        if (groupFilterId != ~0) {
            GroupFilter groupFilter;

            if (groupFilterId >= groupFilterMap.size()) {
                assert groupFilterId == groupFilterMap.size();
                groupFilter = GroupFilter.sRestoreFromBinaryState(reader);
                groupFilterMap.add(groupFilter);
            } else {
                groupFilter = groupFilterMap.get(groupFilterId);
            }

            settings.collisionGroup.groupFilter = groupFilter;
        }

        return settings;
    }

    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        position = JoltUtils.readAlignedVector3(reader);
        rotation = JoltUtils.readQuaternion(reader);
        collisionGroup = CollisionGroup.restoreFromBinaryState(reader);
        objectLayer = reader.readShort();
        motionType = MotionType.values()[reader.readByte()];
        allowDynamicOrKinematic = reader.readByteBoolean();
        motionQuality = MotionQuality.values()[reader.readByte()];
        allowSleeping = reader.readByteBoolean();
        friction = reader.readFloat();
        restitution = reader.readFloat();
        linearDamping = reader.readFloat();
        angularDamping = reader.readFloat();
        maxLinearVelocity = reader.readFloat();
        maxAngularVelocity = reader.readFloat();
        gravityFactor = reader.readFloat();
        overrideMassProperties = OverrideMassProperties.values()[reader.readByte()];
        inertiaMultiplier = reader.readFloat();
        massPropertiesOverride = MassProperties.restoreFromBinaryState(reader);
    }
}
