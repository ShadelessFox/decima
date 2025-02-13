package com.shade.decima.ui.data.viewer.model.dmf.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.shade.decima.ui.data.viewer.model.dmf.data.DMFBuffer;
import com.shade.decima.ui.data.viewer.model.dmf.DMFExporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;

public class JsonBufferSerializer implements JsonSerializer<DMFBuffer> {
    private final DMFExporter dmfExporter;

    public JsonBufferSerializer(DMFExporter dmfExporter) {
        this.dmfExporter = dmfExporter;
    }

    @Override
    public JsonElement serialize(DMFBuffer src, Type type, JsonSerializationContext context) {
        try {
            return src.serialize(dmfExporter, context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
