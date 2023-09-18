package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Quaternion;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.decima.ui.data.viewer.model.utils.Vector3;
import com.shade.util.NotNull;

import java.util.Arrays;

public class DMFTransform {
    public static final DMFTransform IDENTITY = new DMFTransform(new Vector3(0, 0, 0), new Vector3(1, 1, 1), new Quaternion(0, 0, 0, 1));

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DMFTransform that = (DMFTransform) o;
        return Arrays.equals(position, that.position) && Arrays.equals(scale, that.scale) && Arrays.equals(rotation, that.rotation);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(position);
        result = 31 * result + Arrays.hashCode(scale);
        result = 31 * result + Arrays.hashCode(rotation);
        return result;
    }
}
