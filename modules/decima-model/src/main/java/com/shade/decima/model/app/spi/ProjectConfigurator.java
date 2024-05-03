package com.shade.decima.model.app.spi;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.platform.model.ExtensionPoint;
import com.shade.util.NotNull;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface ProjectConfigurator {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ExtensionPoint(ProjectConfigurator.class)
    @interface Registration {
        GameType value();
    }

    @NotNull
    PackfileProvider createPackfileProvider(@NotNull ProjectContainer container);

    @NotNull
    RTTIFactory createRTTIFactory(@NotNull ProjectContainer container) throws IOException;
}
