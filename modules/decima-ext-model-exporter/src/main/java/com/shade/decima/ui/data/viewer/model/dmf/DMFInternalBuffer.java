package com.shade.decima.ui.data.viewer.model.dmf;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.shade.util.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class DMFInternalBuffer extends DMFBuffer {
    public DMFInternalBuffer(@NotNull String name, @NotNull DataProvider provider) {
        super(name, provider);
    }

    @NotNull
    @Override
    public JsonObject serialize(@NotNull DMFExporter exporter, @NotNull JsonSerializationContext context) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (OutputStream os = new DeflaterOutputStream(baos,deflater)) {
            try (InputStream is = provider.openInputStream()) {
                is.transferTo(os);
            }
        }

        final JsonObject object = super.serialize(exporter, context);
        object.addProperty("data", Base64.getEncoder().encodeToString(baos.toByteArray()));
        return object;
    }
}
