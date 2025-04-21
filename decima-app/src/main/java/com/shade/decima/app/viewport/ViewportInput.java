package com.shade.decima.app.viewport;

import com.shade.decima.math.Vec2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles input events for a viewport, including mouse and keyboard events.
 */
final class ViewportInput extends MouseAdapter implements KeyListener, FocusListener {
    private static final Logger log = LoggerFactory.getLogger(ViewportInput.class);

    private final Component viewport;
    private final Robot robot;

    private final Set<Integer> mouseState = new HashSet<>();
    private final Set<Integer> keyState = new HashSet<>();

    private final Point mouseRecent = new Point();
    private final Point mouseDelta = new Point();
    private float mouseWheelDelta;

    public ViewportInput(Component viewport) {
        this.viewport = viewport;
        this.robot = tryCreateRobot();
    }

    public boolean isKeyDown(int keyCode) {
        return keyState.contains(keyCode);
    }

    public boolean isMouseDown(int button) {
        return mouseState.contains(button);
    }

    public Vec2 mousePositionDelta() {
        return new Vec2(mouseDelta.x, mouseDelta.y);
    }

    public float mouseWheelDelta() {
        return mouseWheelDelta;
    }

    public void clear() {
        mouseDelta.x = 0;
        mouseDelta.y = 0;
        mouseWheelDelta = 0;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseState.add(e.getButton());
        mouseRecent.x = e.getX();
        mouseRecent.y = e.getY();
        mouseDelta.x = 0;
        mouseDelta.y = 0;
        SwingUtilities.convertPointToScreen(mouseRecent, viewport);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseState.remove(e.getButton());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        var mouse = e.getLocationOnScreen();
        var bounds = new Rectangle(viewport.getLocationOnScreen(), viewport.getSize());

        // Shrink the bounds in case the viewport covers the entire screen
        bounds.x += 1;
        bounds.y += 1;
        bounds.width -= 2;
        bounds.height -= 2;

        if (robot != null && !bounds.contains(mouse)) {
            mouse.x = wrapAround(mouse.x, bounds.x, bounds.x + bounds.width);
            mouse.y = wrapAround(mouse.y, bounds.y, bounds.y + bounds.height);

            robot.mouseMove(mouse.x, mouse.y);
        } else {
            mouseDelta.x += mouse.x - mouseRecent.x;
            mouseDelta.y += mouse.y - mouseRecent.y;
        }

        mouseRecent.x = mouse.x;
        mouseRecent.y = mouse.y;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseWheelDelta -= (float) e.getPreciseWheelRotation();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keyState.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keyState.remove(e.getKeyCode());
    }

    @Override
    public void focusGained(FocusEvent e) {
        // do nothing
    }

    @Override
    public void focusLost(FocusEvent e) {
        keyState.clear();
        mouseState.clear();
    }

    private static Robot tryCreateRobot() {
        try {
            return new Robot();
        } catch (AWTException e) {
            log.error("Couldn't create robot! Mouse movement won't be constrained within the viewport.", e);
            return null;
        }
    }

    private static int wrapAround(int value, int min, int max) {
        if (value < min) {
            return max - (min - value) % (max - min);
        } else {
            return min + (value - min) % (max - min);
        }
    }
}
