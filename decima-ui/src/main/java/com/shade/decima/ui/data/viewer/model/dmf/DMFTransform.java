package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Quaternion;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.decima.ui.data.viewer.model.utils.Vector3;
import com.shade.util.NotNull;

public class DMFTransform {
    public final double[] position;
    public final double[] scale;
    public final double[] rotation;

    public DMFTransform(@NotNull Vector3 position, @NotNull Vector3 scale, @NotNull Quaternion rotation) {
        this.position = position.toArray();
        this.scale = scale.toArray();
        this.rotation = rotation.toArray();
    }

    public DMFTransform(@NotNull Transform transform) {
        this.position = transform.translation().toArray();
        this.scale = transform.scale().toArray();
        this.rotation = transform.rotation().toArray();
    }

    public DMFTransform(@NotNull Matrix4x4 matrix) {
        this(matrix.toTranslation(), matrix.toScale(), matrix.toQuaternion());
    }
}
