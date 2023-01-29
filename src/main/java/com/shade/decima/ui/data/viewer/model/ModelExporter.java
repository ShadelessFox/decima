package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

public interface ModelExporter {
    @NotNull
    Object export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Exception;
}
