package com.shade.decima.ui.data.viewer.model.dmf.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

public class JsonListSerializer implements JsonSerializer<List<?>> {
    @Override
    public JsonElement serialize(List<?> src, Type type, JsonSerializationContext context) {
        if (src == null || src.isEmpty()) {
            return null;
        }
        final JsonArray result = new JsonArray();
        for (Object o : src) {
            result.add(context.serialize(o));
        }
        return result;
    }
}
