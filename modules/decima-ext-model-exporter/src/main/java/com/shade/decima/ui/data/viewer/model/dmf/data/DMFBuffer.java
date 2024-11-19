package com.shade.decima.ui.data.viewer.model.dmf.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.model.dmf.DMFExporter;
import com.shade.util.NotNull;

import java.io.ByteArrayInputStream;
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

    public record ByteArrayDataProvider(@NotNull byte[] data) implements DMFBuffer.DataProvider {

        @NotNull
        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public int length() {
            return data.length;
        }
    }
    public static class DataSourceDataProvider implements DMFBuffer.DataProvider {
        private final Project project;
        private final HwDataSource dataSource;
        private final int offset;
        private final int length;

        public DataSourceDataProvider(@NotNull Project project, @NotNull HwDataSource dataSource, int offset, int length) {
            this.dataSource = dataSource;
            this.offset = offset;
            this.length = length;
            this.project = project;
        }

        @NotNull
        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(dataSource.getData(project.getPackfileManager(), offset, length));
        }

        @Override
        public int length() {
            return length;
        }
    }

}
