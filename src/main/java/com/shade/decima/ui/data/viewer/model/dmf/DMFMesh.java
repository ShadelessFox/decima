package com.shade.decima.ui.data.viewer.model.dmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMFMesh {
    public final Map<Short, Short> boneRemapTable = new HashMap<>();
    public final List<DMFPrimitive> primitives = new ArrayList<>();

    public DMFPrimitive newPrimitive(int vertexCount, DMFVertexBufferType bufferType, int vertexStart, int vertexEnd,
                                     int indexSize, int indexCount, int indexStart, int indexEnd) {
        DMFPrimitive primitive = new DMFPrimitive(vertexCount, bufferType, vertexStart, vertexEnd,
            indexSize, indexCount, indexStart, indexEnd);
        primitives.add(primitive);
        return primitive;
    }
}
