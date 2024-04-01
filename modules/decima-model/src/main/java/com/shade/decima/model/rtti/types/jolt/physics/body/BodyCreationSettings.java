package com.shade.decima.model.rtti.types.jolt.physics.body;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.decima.model.rtti.types.jolt.physics.collision.CollisionGroup;
import com.shade.decima.model.rtti.types.jolt.physics.collision.GroupFilter;
import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.decima.model.rtti.types.jolt.physics.collision.shape.Shape;
import com.shade.util.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;

public class BodyCreationSettings {
    public enum OverrideMassProperties {
        CalculateMassAndInertia,
        CalculateInertia,
        MassAndInertiaProvided
    }

    public Vector3f position;
    public Quaternionf rotation;
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
        @NotNull ByteBuffer buffer,
        @NotNull List<Shape> shapeMap,
        @NotNull List<PhysicsMaterial> materialMap,
        @NotNull List<GroupFilter> groupFilterMap
    ) {
        final BodyCreationSettings settings = new BodyCreationSettings();
        settings.restoreBinaryState(buffer);
        settings.shape = Shape.sRestoreWithChildren(buffer, shapeMap, materialMap);

        final int groupFilterId = buffer.getInt();
        if (groupFilterId != ~0) {
            final GroupFilter groupFilter;

            if (groupFilterId >= groupFilterMap.size()) {
                assert groupFilterId == groupFilterMap.size();
                groupFilter = GroupFilter.sRestoreFromBinaryState(buffer);
                groupFilterMap.add(groupFilter);
            } else {
                groupFilter = groupFilterMap.get(groupFilterId);
            }

            settings.collisionGroup.groupFilter = groupFilter;
        }

        return settings;
    }

    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        position = JoltUtils.getAlignedVector3(buffer);
        rotation = JoltUtils.getQuaternion(buffer);
        collisionGroup = CollisionGroup.restoreFromBinaryState(buffer);
        objectLayer = buffer.getShort();
        motionType = MotionType.values()[buffer.get()];
        allowDynamicOrKinematic = buffer.get() != 0;
        motionQuality = MotionQuality.values()[buffer.get()];
        allowSleeping = buffer.get() != 0;
        friction = buffer.getFloat();
        restitution = buffer.getFloat();
        linearDamping = buffer.getFloat();
        angularDamping = buffer.getFloat();
        maxLinearVelocity = buffer.getFloat();
        maxAngularVelocity = buffer.getFloat();
        gravityFactor = buffer.getFloat();
        overrideMassProperties = OverrideMassProperties.values()[buffer.get()];
        inertiaMultiplier = buffer.getFloat();
        massPropertiesOverride = MassProperties.restoreFromBinaryState(buffer);
    }
}
