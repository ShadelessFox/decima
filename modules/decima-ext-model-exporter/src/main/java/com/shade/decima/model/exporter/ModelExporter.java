package com.shade.decima.model.exporter;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.io.Writer;

public interface ModelExporter {
    void export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName,
        @NotNull Writer writer
    ) throws Exception;
}
