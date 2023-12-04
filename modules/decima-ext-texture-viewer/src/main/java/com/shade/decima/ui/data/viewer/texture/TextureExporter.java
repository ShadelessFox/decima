package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public interface TextureExporter {
    enum Option {
        INCLUDE_MIPS("Include all mips", true),
        INCLUDE_SLICES("Include all slices", true);

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

    void export(@NotNull ProgressMonitor monitor, @NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException;

    boolean supportsImage(@NotNull ImageProvider provider);

    boolean supportsOption(@NotNull Option option);

    @NotNull
    String getExtension();
}
