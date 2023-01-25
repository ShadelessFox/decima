package com.shade.decima.ui.data.viewer.model.model.dmf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DMFLodModel extends DMFModel {
    public final List<Lod> lods = new ArrayList<>();

    public DMFLodModel() {
        type = "LOD";
    }

    public void addLod(@NotNull DMFNode model, int id, float distance) {
        lods.add(new Lod(model, id, distance));
    }

    public record Lod(@NotNull DMFNode model, int id, float distance) {}
}
