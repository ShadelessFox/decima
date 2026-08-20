package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.registry.providers.ExternalTypeProvider;
import com.shade.decima.model.rtti.registry.providers.InternalTypeProvider;
import com.shade.decima.model.rtti.registry.providers.JavaTypeProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HZDRTypeRegistryTest {
    @Test
    void loadsMetadataAndCompatibleHandlers() throws IOException {
        final Path unused = Path.of("");
        final ProjectContainer container = new ProjectContainer(
            UUID.randomUUID(), "HZDR", GameType.HZDR, unused, unused, unused
        );
        final RTTITypeRegistry registry = new RTTITypeRegistry(container, List.of(
            new InternalTypeProvider(), new ExternalTypeProvider(), new JavaTypeProvider()
        ));

        assertEquals("Texture", registry.find("Texture").getTypeName());
        assertNotNull(registry.<RTTIClass>find("LocalizedTextResource").getMessage("MsgReadBinary").getHandler());
        assertNotNull(registry.<RTTIClass>find("PhysicsShapeResource").getMessage("MsgReadBinary").getHandler());
    }
}
