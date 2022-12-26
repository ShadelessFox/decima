package com.shade.decima.ui.data.viewer.model.dmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMFMesh {
    public Map<Short,Short> boneRemapTable = new HashMap<>();
    public List<DMFPrimitive> primitives = new ArrayList<>();


    public DMFPrimitive newPrimitive() {
        DMFPrimitive primitive = new DMFPrimitive();
        primitives.add(primitive);
        return primitive;
    }
}
