package com.shade.decima.ui.data.viewer.model.dmf.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joml.Vector2ic;

import java.lang.reflect.Type;

public class JsonVector2icSerializer implements JsonSerializer<Vector2ic> {
    @Override
    public JsonElement serialize(Vector2ic src, Type type, JsonSerializationContext context) {
        JsonArray jsonObject = new JsonArray();
        jsonObject.add(src.x());
        jsonObject.add(src.y());
        return jsonObject;
    }
}
