package com.shade.decima.ui.data.viewer.model.dmf.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joml.Vector3fc;

import java.lang.reflect.Type;

public class JsonVector3fcSerializer implements JsonSerializer<Vector3fc> {
    @Override
    public JsonElement serialize(Vector3fc src, Type type, JsonSerializationContext context) {
        JsonArray jsonObject = new JsonArray();
        jsonObject.add(src.x());
        jsonObject.add(src.y());
        jsonObject.add(src.z());
        return jsonObject;
    }
}
