package com.shade.decima.ui.data.viewer.model.dmf;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;

public abstract class DMFBuffer {
    public final String name;
    protected final DataProvider provider;

    public DMFBuffer(@NotNull String name, @NotNull DataProvider provider) {
        this.name = name;
        this.provider = provider;
    }

    @NotNull
    public JsonObject serialize(@NotNull DMFExporter exporter, @NotNull JsonSerializationContext context) throws IOException {
        final JsonObject object = new JsonObject();
        object.addProperty("name", name);
        object.addProperty("size", provider.length());
        return object;
    }

    public interface DataProvider {
        @NotNull
        InputStream openInputStream() throws IOException;

        int length();
    }
}
