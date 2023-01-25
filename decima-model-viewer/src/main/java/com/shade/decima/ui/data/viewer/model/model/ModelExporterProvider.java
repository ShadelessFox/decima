package com.shade.decima.ui.data.viewer.model.model;

import com.shade.decima.model.app.Project;
import com.shade.util.NotNull;

import java.nio.file.Path;

public interface ModelExporterProvider {
    @NotNull
    ModelExporter create(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath);

    @NotNull
    String getExtension();

    @NotNull
    String getName();
}
