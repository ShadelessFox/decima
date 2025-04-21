package com.shade.decima.game.hfw.data.jolt.core;

import com.shade.util.NotNull;
import com.shade.util.hash.HashFunction;

import java.util.HashMap;
import java.util.Map;

public class Factory {
    private static final String[] TYPE_NAMES = {
        "SkeletalAnimation",
        "Skeleton",
        "CompoundShapeSettings",
        "StaticCompoundShapeSettings",
        "MutableCompoundShapeSettings",
        "TriangleShapeSettings",
        "SphereShapeSettings",
        "BoxShapeSettings",
        "CapsuleShapeSettings",
        "TaperedCapsuleShapeSettings",
        "CylinderShapeSettings",
        "ScaledShapeSettings",
        "MeshShapeSettings",
        "ConvexHullShapeSettings",
        "HeightFieldShapeSettings",
        "RotatedTranslatedShapeSettings",
        "OffsetCenterOfMassShapeSettings",
        "RagdollSettings",
        "PointConstraintSettings",
        "SixDOFConstraintSettings",
        "SliderConstraintSettings",
        "SwingTwistConstraintSettings",
        "DistanceConstraintSettings",
        "HingeConstraintSettings",
        "FixedConstraintSettings",
        "ConeConstraintSettings",
        "PathConstraintSettings",
        "VehicleConstraintSettings",
        "WheeledVehicleControllerSettings",
        "PathConstraintPath",
        "PathConstraintPathHermite",
        "MotorSettings",
        "PhysicsScene",
        "PhysicsMaterial",
        "PhysicsMaterialSimple",
        "GroupFilter",
        "GroupFilterTable",
    };

    private static final Map<Integer, String> HASH_TO_TYPE_NAME_MAP = new HashMap<>();

    static {
        for (String name : TYPE_NAMES) {
            HASH_TO_TYPE_NAME_MAP.put(computeHash(name), name);
        }
    }

    @NotNull
    public static String getTypeName(int hash) {
        var name = HASH_TO_TYPE_NAME_MAP.get(hash);
        if (name == null) {
            throw new IllegalArgumentException("Unknown type hash: %#010x".formatted(hash));
        }
        return name;
    }

    @SuppressWarnings("UseHashCodeMethodInspection")
    private static int computeHash(@NotNull String name) {
        var hash = HashFunction.fnv1a().hash(name).asLong();
        return (int) (hash ^ (hash >>> 32));
    }
}
