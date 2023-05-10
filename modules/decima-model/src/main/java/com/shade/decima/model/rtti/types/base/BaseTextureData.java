package com.shade.decima.model.rtti.types.base;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.HwTextureData;
import com.shade.decima.model.rtti.types.java.RTTIField;

public abstract class BaseTextureData implements HwTextureData {
    @RTTIField(type = @Type(name = "uint32"))
    public int remainingDataSize;
    @RTTIField(type = @Type(name = "uint32"))
    public int internalDataSize;
    @RTTIField(type = @Type(name = "uint32"))
    public int externalDataSize;
    @RTTIField(type = @Type(name = "uint32"))
    public int externalMipCount;
    @RTTIField(type = @Type(type = HwDataSource.class))
    public RTTIObject externalData;
    @RTTIField(type = @Type(name = "Array<uint8>"))
    public byte[] internalData;

    @Override
    public byte[] getInternalData() {
        return internalData;
    }

    @Override
    public HwDataSource getExternalData() {
        return externalData != null ? externalData.cast() : null;
    }

    @Override
    public int getExternalMipCount() {
        return externalMipCount;
    }
}
