package com.shade.decima.model.rtti.types.jolt.physics.ragdoll;

import com.shade.decima.model.rtti.types.jolt.physics.body.BodyCreationSettings;
import com.shade.decima.model.rtti.types.jolt.physics.collision.GroupFilter;
import com.shade.decima.model.rtti.types.jolt.physics.collision.shape.Shape;
import com.shade.decima.model.rtti.types.jolt.physics.constraints.ConstraintSettings;
import com.shade.decima.model.rtti.types.jolt.physics.constraints.TwoBodyConstraintSettings;
import com.shade.decima.model.rtti.types.jolt.skeleton.Skeleton;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class RagdollSettings {
    public static class Part {
        private BodyCreationSettings body;
        private TwoBodyConstraintSettings constraint;
    }

    private Skeleton skeleton;
    private Part[] parts;

    @NotNull
    public static RagdollSettings sRestoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final List<Shape> shapeMap = new ArrayList<>(1024);
        final List<GroupFilter> groupFilterMap = new ArrayList<>(128);

        final RagdollSettings settings = new RagdollSettings();
        settings.skeleton = Skeleton.restoreFromBinaryState(buffer);
        settings.parts = BufferUtils.getObjects(buffer, buffer.getInt(), Part[]::new, buf -> {
            final Part part = new Part();
            part.body = BodyCreationSettings.sRestoreWithChildren(buf, shapeMap, groupFilterMap);

            final boolean hasConstraint = buf.get() != 0;
            if (hasConstraint) {
                part.constraint = (TwoBodyConstraintSettings) ConstraintSettings.restoreFromBinaryState(buf);
            }

            return part;
        });

        return settings;
    }
}
