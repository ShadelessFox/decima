package com.shade.decima.game.hfw.data.jolt.physics.ragdoll;

import com.shade.decima.game.hfw.data.jolt.physics.body.BodyCreationSettings;
import com.shade.decima.game.hfw.data.jolt.physics.constraints.TwoBodyConstraintSettings;

public class Ragdoll {
    public static class Part extends BodyCreationSettings {
        public TwoBodyConstraintSettings toParent;
    }
}
