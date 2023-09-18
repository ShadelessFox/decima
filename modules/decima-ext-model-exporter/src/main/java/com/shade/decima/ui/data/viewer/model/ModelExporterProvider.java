package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.app.Project;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.Set;

public interface ModelExporterProvider {
    enum Option {
        EXPORT_LODS("Export LODs", false),
        EXPORT_TEXTURES("Export textures", false),
        EMBED_BUFFERS("Embed buffers", true),
        EMBED_TEXTURES("Embed textures", true),
        USE_INSTANCING("Use mesh instancing", true);

        private final String label;
        private final boolean enabledByDefault;

        Option(@NotNull String label, boolean enabledByDefault) {
            this.label = label;
            this.enabledByDefault = enabledByDefault;
        }

        @NotNull
        public String getLabel() {
            return label;
        }

        public boolean isEnabledByDefault() {
            return enabledByDefault;
        }
    }

    @NotNull
    ModelExporter create(@NotNull Project project, @NotNull Set<Option> options, @NotNull Path outputPath);

    boolean supportsOption(@NotNull Option option);

    @NotNull
    String getExtension();

    @NotNull
    String getName();
}
