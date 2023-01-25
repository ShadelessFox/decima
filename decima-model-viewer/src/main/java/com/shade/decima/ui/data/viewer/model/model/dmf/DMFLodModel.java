package com.shade.decima.ui.data.viewer.model.model.dmf;

import java.util.ArrayList;
import java.util.List;

public class DMFLodModel extends DMFModel {
    public static class Lod {
        public DMFNode model;
        public int id;
        public float distance;

        public Lod(DMFNode model, int id, float distance) {
            this.model = model;
            this.id = id;
            this.distance = distance;
        }
    }


    List<Lod> lods = new ArrayList<>();

    public DMFLodModel() {
        super();
        type = "LOD";
    }

    public void addLod(DMFNode model, int id, float distance) {
        lods.add(new Lod(model, id, distance));
    }

}
