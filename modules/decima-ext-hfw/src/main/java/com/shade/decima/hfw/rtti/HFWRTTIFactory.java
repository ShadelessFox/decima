package com.shade.decima.hfw.rtti;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HFWRTTIFactory extends RTTIFactory {
    public HFWRTTIFactory(@NotNull ProjectContainer container) throws IOException {
        super(container);
    }

    @Override
    protected long computeHash(@NotNull RTTIType<?> type) {
        assert type instanceof RTTIClass;
        final String name = "00000001_" + type.getTypeName();
        return MurmurHash3.mmh3(name.getBytes(StandardCharsets.UTF_8))[0];
    }
}