package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;
import org.joml.*;

import java.util.Arrays;

public class DMFTransform {
    public static final DMFTransform IDENTITY = new DMFTransform(new double[]{0, 0, 0}, new double[]{1, 1, 1}, new double[]{0, 0, 0, 1});

    public final double[] position;
    public final double[] scale;
    public final double[] rotation;

    public DMFTransform(@NotNull double[] position, @NotNull double[] scale, @NotNull double[] rotation) {
        this.position = position;
        this.scale = scale;
        this.rotation = rotation;
    }

    public DMFTransform(@NotNull Matrix4dc matrix) {
        final Vector3dc translation = matrix.getTranslation(new Vector3d());
        final Vector3dc scale = matrix.getScale(new Vector3d());
        final Quaterniondc rotation = matrix.getNormalizedRotation(new Quaterniond());
        this.position = new double[]{translation.x(), translation.y(), translation.z()};
        this.scale = new double[]{scale.x(), scale.y(), scale.z()};
        this.rotation = new double[]{rotation.x(), rotation.y(), rotation.z(), rotation.w()};
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
