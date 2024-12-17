package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.data.jolt.physics.ragdoll.RagdollSettings;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PhysicsRagdollResourceCallback implements ExtraBinaryDataCallback<PhysicsRagdollResourceCallback.PhysicsRagdollData> {
    public interface PhysicsRagdollData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PhysicsRagdollData object) throws IOException {
        // FIXME: Skipped for now
        var ragdoll = RagdollSettings.sRestoreFromBinaryState(reader);
    }
}
