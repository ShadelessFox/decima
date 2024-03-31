package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.util.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public class ShapeFunctions {
    private static final Map<ShapeSubType, ShapeFunctions> REGISTRY = Map.of(
        ShapeSubType.Box, new ShapeFunctions(BoxShape::new),
        ShapeSubType.Capsule, new ShapeFunctions(CapsuleShape::new),
        ShapeSubType.ConvexHull, new ShapeFunctions(ConvexHullShape::new),
        ShapeSubType.Cylinder, new ShapeFunctions(CylinderShape::new),
        ShapeSubType.Mesh, new ShapeFunctions(MeshShape::new),
        ShapeSubType.RotatedTranslated, new ShapeFunctions(RotatedTranslatedShape::new),
        ShapeSubType.Scaled, new ShapeFunctions(ScaledShape::new),
        ShapeSubType.Sphere, new ShapeFunctions(SphereShape::new),
        ShapeSubType.StaticCompound, new ShapeFunctions(StaticCompoundShape::new),
        ShapeSubType.TaperedCapsule, new ShapeFunctions(TaperedCapsuleShape::new)
    );

    private final Supplier<Shape> constructor;

    public ShapeFunctions(@NotNull Supplier<Shape> constructor) {
        this.constructor = constructor;
    }

    @NotNull
    public static ShapeFunctions get(@NotNull ShapeSubType shapeSubType) {
        final ShapeFunctions functions = REGISTRY.get(shapeSubType);
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
