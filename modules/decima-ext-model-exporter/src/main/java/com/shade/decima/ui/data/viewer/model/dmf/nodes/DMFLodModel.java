package com.shade.decima.ui.data.viewer.model.dmf.nodes;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DMFLodModel extends DMFNode {
    public final List<Lod> lods = new ArrayList<>();

    public DMFLodModel(@NotNull String name) {
        super(name, DMFNodeType.LOD);
    }

    public void addLod(@NotNull DMFNode model, float distance) {
        lods.add(new Lod(model, lods.size(), distance));
    }

    public record Lod(@NotNull DMFNode model, int id, float distance) {}

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && lods.isEmpty();
    }
}
