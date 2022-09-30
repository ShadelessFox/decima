package com.shade.decima.ui.data.viewer.mesh.dmf;

import com.shade.decima.ui.data.viewer.mesh.utils.Quaternion;
import com.shade.decima.ui.data.viewer.mesh.utils.Transform;
import com.shade.decima.ui.data.viewer.mesh.utils.Vector3;

public class DMFTransform {
    public double[] position;
    public double[] scale;
    public double[] rotation;

    public static DMFTransform FromTransform(Transform transform) {
        DMFTransform dmfTransform = new DMFTransform();
        dmfTransform.position = transform.getTranslation();
        dmfTransform.scale = transform.getScale();
        dmfTransform.rotation = transform.getRotation();
        return dmfTransform;
    }

    public DMFTransform() {
    }

    public DMFTransform(Vector3 position, Vector3 scale, Quaternion rotation) {
        this.position = position.toArray();
        this.scale = scale.toArray();
        this.rotation = rotation.toArray();
    }
}
