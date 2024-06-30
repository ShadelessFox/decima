package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.data.viewer.model.ModelExporter;
import com.shade.decima.ui.data.viewer.model.ModelExporterProvider;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.Set;

public class DMFExporterProvider implements ModelExporterProvider {
    @NotNull
    @Override
    public ModelExporter create(@NotNull Project project, @NotNull Set<Option> options, @NotNull Path outputPath) {
        return new DMFExporter(project, options, outputPath);
    }

    @Override
    public boolean supportsOption(@NotNull Option option) {
        return true;
    }

    @NotNull
    @Override
    public String getExtension() {
        return "dmf";
    }

    @NotNull
    @Override
    public String getName() {
        return "DMF Scene";
    }
}
