package com.shade.decima.ui.data.viewer.model.model.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFTexture {
    public String name;
    public DMFDataType dataType;
    public int bufferSize;
    public String usageType;
    public Map<String, String> metadata = new HashMap<>();


    public DMFTexture(String name) {
        this.name = name;
    }

    public static DMFTexture nonExportableTexture(String name) {
        DMFTexture texture = new DMFTexture(name);
        texture.dataType = DMFDataType.UNSUPPORTED;
        texture.bufferSize = -1;
        return texture;
    }
}
