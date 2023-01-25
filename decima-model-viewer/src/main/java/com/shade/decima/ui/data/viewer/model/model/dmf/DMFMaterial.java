package com.shade.decima.ui.data.viewer.model.model.dmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMFMaterial {
    public String name;
    public String type;
    public final Map<String, Integer> textureIds = new HashMap<>();
    public final List<DMFTextureDescriptor> textureDescriptors = new ArrayList<>();
}
