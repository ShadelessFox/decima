package com.shade.decima.model.packfile.edit;

import com.shade.decima.model.packfile.resource.Resource;
import com.shade.util.NotNull;

import java.io.IOException;

public interface Change {
    /**
     * Merges current, old change with other, new {@code change}.
     *
     * @param other other change to merge current change with
     * @return merged change
     */
    @NotNull
    Change merge(@NotNull Change other);

    @NotNull
    Resource toResource() throws IOException;
}
