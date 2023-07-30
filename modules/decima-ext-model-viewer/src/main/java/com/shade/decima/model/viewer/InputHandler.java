package com.shade.decima.model.viewer;

import com.shade.util.NotNull;
import org.joml.Vector2f;

public interface InputHandler {
    boolean isKeyDown(int keyCode);

    boolean isMouseDown(int mouseButton);

    @NotNull
    Vector2f getMouseOrigin();

    @NotNull
    Vector2f getMousePosition();

    float getMouseWheelRotation();
}
