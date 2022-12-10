package com.shade.decima.ui.data.viewer.model;

public class ExportSettings {
    public boolean exportTextures;
    public boolean embedBuffers;
    public boolean embedTextures;

    public ExportSettings(boolean exportTextures, boolean embedBuffers, boolean embedTextures) {
        this.exportTextures = exportTextures;
        this.embedBuffers = embedBuffers;
        this.embedTextures = embedTextures;
    }
}
