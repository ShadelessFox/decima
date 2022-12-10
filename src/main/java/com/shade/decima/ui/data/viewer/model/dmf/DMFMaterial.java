package com.shade.decima.ui.data.viewer.model.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFMaterial {
    public String name;
    public float roughness = 0.8f;
    public float specular = 0.5f;
    public float metalnes = 0.0f;
    public Map<String, Integer> textureIds=new HashMap<>();
    public String type;
}
