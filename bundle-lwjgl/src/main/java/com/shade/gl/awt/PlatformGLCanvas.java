package com.shade.gl.awt;

import java.awt.*;

/**
 * Interface for platform-specific implementations of {@link AWTGLCanvas}.
 *
 * @author Kai Burjack
 */
sealed interface PlatformGLCanvas permits PlatformLinuxGLCanvas, PlatformWin32GLCanvas {
    long create(Canvas canvas, GLData data, GLData effective) throws AWTException;

    boolean makeCurrent(long context);

    boolean swapBuffers();

    void lock() throws AWTException;

    void unlock() throws AWTException;

    void dispose();
}
