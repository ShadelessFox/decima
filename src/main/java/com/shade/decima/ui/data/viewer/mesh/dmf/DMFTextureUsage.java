package com.shade.decima.ui.data.viewer.mesh.dmf;

public enum DMFTextureUsage {
    SPECULAR_ALBEDO(1507191729),
    SPECULAR_NORMAL(1437640823),
    SPECULAR_AO_SPECULAR(1313470302),
    SPECULAR_AO_SPEC_ROUGHNESS(1687922523),
    ALBEDO(361825343),
    NORMAL(1060590963),
    COMPRESS_ALBEDO(313684873),
    COMPRESS_NORMAL(1196843070),
    STRETCH_ALBEDO(785128763),
    STRETCH_NORMAL(524327456);

    public final int value;

    DMFTextureUsage(int value) {

        this.value = value;
    }

    public static boolean contains(int value) {
        for (DMFTextureUsage dmfTextureUsage : DMFTextureUsage.values()) {
            if (dmfTextureUsage.value == value) {
                return true;
            }
        }
        return false;
    }

    public static DMFTextureUsage fromInt(int value) {
        for (DMFTextureUsage dmfTextureUsage : DMFTextureUsage.values()) {
            if (dmfTextureUsage.value == value) {
                return dmfTextureUsage;
            }
        }
        return null;
    }
}
