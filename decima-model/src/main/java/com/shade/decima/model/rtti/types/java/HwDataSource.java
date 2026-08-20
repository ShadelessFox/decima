package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.archive.ArchiveManager;
import com.shade.util.NotNull;

import java.io.IOException;

public interface HwDataSource extends HwType {
    @NotNull
    byte[] getData(@NotNull ArchiveManager manager) throws IOException;

    @NotNull
    byte[] getData(@NotNull ArchiveManager manager, int offset, int length) throws IOException;

    @NotNull
    String getLocation();

    int getOffset();

    int getLength();
}
