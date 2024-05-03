package com.shade.decima.hfw.project;

import com.shade.decima.hfw.rtti.HFWRTTIFactory;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.spi.ProjectConfigurator;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.io.IOException;

@ProjectConfigurator.Registration(GameType.HFW)
public class HFWProjectConfigurator implements ProjectConfigurator {
    @NotNull
    @Override
    public PackfileProvider createPackfileProvider(@NotNull ProjectContainer container) {
        return new HFWPackfileProvider();
    }

    @NotNull
    @Override
    public RTTIFactory createRTTIFactory(@NotNull ProjectContainer container) throws IOException {
        return new HFWRTTIFactory(container);
    }
}
