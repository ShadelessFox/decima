package com.shade.decima.ui.data.viewer.model.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFTexture {
    public String name;
    public DMFDataType dataType;
    public int bufferSize;
    public long usageType;
    public Map<String, String> metadata = new HashMap<>();


    public static DMFTexture nonExportableTexture(String name) {
        DMFTexture texture = new DMFTexture();
        texture.dataType = DMFDataType.UNSUPPORTED;
        texture.bufferSize = -1;
        texture.name = name;
        return texture;
    }
}
