package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

public interface ModelExporter {
    void export(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull SeekableByteChannel channel
    ) throws IOException;
}
