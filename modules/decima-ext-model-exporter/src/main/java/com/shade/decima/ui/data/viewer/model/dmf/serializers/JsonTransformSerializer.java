package com.shade.decima.ui.data.viewer.model.dmf.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.shade.decima.ui.data.viewer.model.dmf.data.DMFTransform;

import java.lang.reflect.Type;

public class JsonTransformSerializer implements JsonSerializer<DMFTransform> {
    @Override
    public JsonElement serialize(DMFTransform src, Type type, JsonSerializationContext context) {
        if (DMFTransform.IDENTITY.equals(src)) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("position", context.serialize(src.position));
        jsonObject.add("scale", context.serialize(src.scale));
        jsonObject.add("rotation", context.serialize(src.rotation));
        return jsonObject;
    }
}
