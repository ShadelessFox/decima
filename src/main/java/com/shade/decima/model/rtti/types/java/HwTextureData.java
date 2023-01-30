package com.shade.decima.model.rtti.types.java;

public interface HwTextureData extends HwType {
    byte[] getInternalData();

    HwDataSource getExternalData();

    int getExternalMipCount();
}
