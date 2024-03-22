package com.shade.decima.ui.bookmarks;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.util.NotNull;

import java.lang.reflect.Type;

public record Bookmark(@NotNull Location location, @NotNull String name) {
    @JsonAdapter(LocationJsonAdapter.class)
    public record Location(@NotNull String project, @NotNull String packfile, @NotNull String path) {
        @Override
        public String toString() {
            return project + ':' + packfile + ':' + path;
        }

        @NotNull
        public static Location of(@NotNull String value) {
            final String[] parts = value.split(":");
            return new Location(parts[0], parts[1], parts[2]);
        }

        @NotNull
        public static Location of(@NotNull NavigatorFileNode node) {
            return new Location(node.getProjectContainer().getId().toString(), node.getArchive().getId(), node.getPath().full());
        }
    }

    private static class LocationJsonAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
        @Override
        public JsonElement serialize(Location src, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Location.of(json.getAsString());
        }
    }

}
