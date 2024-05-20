package com.shade.gl;

import com.shade.util.NotNull;
import org.lwjgl.opengl.GL43;

/**
 * Creates a named debug group for the current OpenGL context.
 * <p>
 * <b>Note</b>: you can't reuse the same instance of this class. It's meant to be used in a try-with-resources block.
 * <blockquote><pre>
 * try (var group = new DebugGroup("My Group")) {
 *     // OpenGL calls
 * }</pre>
 * </blockquote>
 *
 * @param name the name of the debug group
 */
public record DebugGroup(@NotNull String name) implements AutoCloseable {
    public DebugGroup {
        GL43.glPushDebugGroup(GL43.GL_DEBUG_SOURCE_APPLICATION, 0, name);
    }

    @Override
    public void close() {
        GL43.glPopDebugGroup();
    }
}
