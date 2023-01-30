package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.util.NotNull;

import java.io.IOException;

public interface HwDataSource extends HwType {
    @NotNull
    byte[] getData(@NotNull PackfileManager manager) throws IOException;

    int getOffset();

    int getLength();
}
