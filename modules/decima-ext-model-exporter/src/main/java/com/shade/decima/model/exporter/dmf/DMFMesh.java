package com.shade.decima.model.exporter.dmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMFMesh {
    public final Map<Short, Short> boneRemapTable = new HashMap<>();
    public final List<DMFPrimitive> primitives = new ArrayList<>();
}
