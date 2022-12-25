package com.shade.decima.model.packfile.edit;

import com.shade.decima.model.packfile.resource.Resource;
import com.shade.util.NotNull;

import java.io.IOException;

public interface Change {
    @NotNull
    Resource toResource() throws IOException;
}
