package com.shade.decima.ui.data.viewer.model.dmf;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.shade.decima.ui.data.viewer.model.DMFExporter;
import com.shade.util.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DMFExternalBuffer extends DMFBuffer {
    public final String path;

    public DMFExternalBuffer(@NotNull String name, @NotNull String path, @NotNull DataProvider provider) {
        super(name, provider);
        this.path = path;
    }

    @NotNull
    @Override
    public JsonObject serialize(@NotNull DMFExporter exporter, @NotNull JsonSerializationContext context) throws IOException {
        final Path resolved = exporter.getBuffersPath().resolve(path);
        Files.createDirectories(resolved.getParent());

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(resolved))) {
            try (InputStream is = provider.openInputStream()) {
                is.transferTo(os);
            }
        }

        final JsonObject object = super.serialize(exporter, context);
        object.addProperty("path", path);
        return object;
    }
}
