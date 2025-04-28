package com.shade.gl.awt;

import org.lwjgl.system.Platform;

import javax.swing.*;
import java.awt.*;

/**
 * An AWT {@link Canvas} that supports to be drawn on using OpenGL.
 *
 * @author Kai Burjack
 */
public abstract class AWTGLCanvas extends Canvas {
    private final PlatformGLCanvas platformCanvas = createPlatformCanvas();

    private static PlatformGLCanvas createPlatformCanvas() {
        return switch (Platform.get()) {
            case WINDOWS -> new PlatformWin32GLCanvas();
            case LINUX -> new PlatformLinuxGLCanvas();
            default -> throw new UnsupportedOperationException("Platform " + Platform.get() + " not yet supported");
        };
    }

    protected long context;
    protected final GLData data;
    protected final GLData effective = new GLData();

    protected AWTGLCanvas(GLData data) {
        this.data = data;
    }

    @Override
    public void removeNotify() {
        if (context != 0) {
            disposeGL();
            platformCanvas.dispose();
            context = 0;
        }

        super.removeNotify();
    }

    public void render() {
        ensureEDT();
        beforeRender();
        try {
            paintGL();
        } finally {
            afterRender();
        }
    }

    private void beforeRender() {
        boolean needsInitialization = false;
        if (context == 0L) {
            try {
                context = platformCanvas.create(this, data, effective);
                needsInitialization = true;
            } catch (AWTException e) {
                throw new RuntimeException("Exception while creating the OpenGL context", e);
            }
        }
        try {
            platformCanvas.lock(); // <- MUST lock on Linux
        } catch (AWTException e) {
            throw new RuntimeException("Failed to lock Canvas", e);
        }
        platformCanvas.makeCurrent(context);
        if (needsInitialization) {
            initGL();
        }
    }

    private void afterRender() {
        platformCanvas.makeCurrent(0L);
        try {
            platformCanvas.unlock(); // <- MUST unlock on Linux
        } catch (AWTException e) {
            throw new RuntimeException("Failed to unlock Canvas", e);
        }
    }

    /**
     * Will be called once after the OpenGL has been created.
     */
    protected abstract void initGL();

    /**
     * Will be called whenever the {@link Canvas} needs to paint itself.
     */
    protected abstract void paintGL();

    /**
     * Will be called once before the OpenGL context is disposed.
     */
    protected abstract void disposeGL();

    protected final void swapBuffers() {
        ensureEDT();
        platformCanvas.swapBuffers();
    }

    private void ensureEDT() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("This method must be called on the EDT");
        }
    }
}
