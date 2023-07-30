package com.shade.decima.model.exporter.dmf;

public class DMFTextureDescriptor {
    public int textureId = -1;
    public String channels = "";

    public DMFTextureDescriptor(int textureId, String channels, String usageType) {
        this.textureId = textureId;
        this.channels = channels;
        this.usageType = usageType;
    }

    public String usageType;
}
