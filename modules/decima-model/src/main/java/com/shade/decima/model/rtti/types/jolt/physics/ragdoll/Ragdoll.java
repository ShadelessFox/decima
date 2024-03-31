package com.shade.decima.model.rtti.types.jolt.physics.ragdoll;

import com.shade.decima.model.rtti.types.jolt.physics.body.BodyCreationSettings;
import com.shade.decima.model.rtti.types.jolt.physics.constraints.TwoBodyConstraintSettings;
import com.shade.util.Nullable;

public class Ragdoll {
    public static class Part extends BodyCreationSettings {
        private TwoBodyConstraintSettings toParent;

        public void setToParent(@Nullable TwoBodyConstraintSettings toParent) {
            this.toParent = toParent;
        }
    }
}
