package com.shade.decima.hfw.data.jolt.physics.collision.shape;

import com.shade.util.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public class ShapeFunctions {
    private static final Map<ShapeSubType, ShapeFunctions> factory = Map.ofEntries(
        Map.entry(ShapeSubType.Box, new ShapeFunctions(BoxShape::new)),
        Map.entry(ShapeSubType.Capsule, new ShapeFunctions(CapsuleShape::new)),
        Map.entry(ShapeSubType.ConvexHull, new ShapeFunctions(ConvexHullShape::new)),
        Map.entry(ShapeSubType.Cylinder, new ShapeFunctions(CylinderShape::new)),
        Map.entry(ShapeSubType.HeightField, new ShapeFunctions(HeightFieldShape::new)),
        Map.entry(ShapeSubType.Mesh, new ShapeFunctions(MeshShape::new)),
        Map.entry(ShapeSubType.OffsetCenterOfMass, new ShapeFunctions(OffsetCenterOfMassShape::new)),
        Map.entry(ShapeSubType.RotatedTranslated, new ShapeFunctions(RotatedTranslatedShape::new)),
        Map.entry(ShapeSubType.Scaled, new ShapeFunctions(ScaledShape::new)),
        Map.entry(ShapeSubType.Sphere, new ShapeFunctions(SphereShape::new)),
        Map.entry(ShapeSubType.StaticCompound, new ShapeFunctions(StaticCompoundShape::new)),
        Map.entry(ShapeSubType.TaperedCapsule, new ShapeFunctions(TaperedCapsuleShape::new))
    );

    private final Supplier<Shape> constructor;

    public ShapeFunctions(@NotNull Supplier<Shape> constructor) {
        this.constructor = constructor;
    }

    @NotNull
    public static ShapeFunctions get(@NotNull ShapeSubType shapeSubType) {
        final ShapeFunctions functions = factory.get(shapeSubType);
        if (functions == null) {
            throw new IllegalArgumentException("Unknown shape subtype: " + shapeSubType);
        }
        return functions;
    }

    @NotNull
    public Shape construct() {
        return constructor.get();
    }
}
