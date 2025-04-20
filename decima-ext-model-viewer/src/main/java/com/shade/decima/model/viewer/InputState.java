package com.shade.decima.model.viewer;

import com.shade.util.NotNull;
import org.joml.Vector2f;

public interface InputState {
    boolean isKeyDown(int keyCode);

    boolean isMouseDown(int mouseButton);

    @NotNull
    Vector2f getMousePositionDelta();

    float getMouseWheelRotationDelta();
}
