package com.shade.decima.model.viewer;

import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import java.io.IOException;

public interface Renderer extends Disposable {
    void setup() throws IOException;

    void update(float dt, @NotNull InputHandler handler, @NotNull MeshViewerCanvas canvas);
}
