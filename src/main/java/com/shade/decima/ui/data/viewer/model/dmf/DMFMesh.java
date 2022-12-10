package com.shade.decima.ui.data.viewer.model.dmf;

import java.util.ArrayList;
import java.util.List;

public class DMFMesh {
    public List<DMFPrimitive> primitives = new ArrayList<>();


    public DMFPrimitive newPrimitive() {
        DMFPrimitive primitive = new DMFPrimitive();
        primitives.add(primitive);
        return primitive;
    }
}
