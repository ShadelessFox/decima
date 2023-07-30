package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMFMaterial {
    public final String name;
    public String type;
    public final Map<String, Integer> textureIds = new HashMap<>();
    public final List<DMFTextureDescriptor> textureDescriptors = new ArrayList<>();

    public DMFMaterial(@NotNull String name) {
        this.name = name;
    }
}
