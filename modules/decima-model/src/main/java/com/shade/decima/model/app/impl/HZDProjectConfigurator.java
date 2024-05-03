package com.shade.decima.model.app.impl;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.spi.ProjectConfigurator;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.io.IOException;

@ProjectConfigurator.Registration(GameType.DS)
public class HZDProjectConfigurator implements ProjectConfigurator {
    @NotNull
    @Override
    public PackfileProvider createPackfileProvider(@NotNull ProjectContainer container) {
        return new HZDPackfileProvider();
    }

    @NotNull
    @Override
    public RTTIFactory createRTTIFactory(@NotNull ProjectContainer container) throws IOException {
        return new RTTIFactory(container);
    }
}
