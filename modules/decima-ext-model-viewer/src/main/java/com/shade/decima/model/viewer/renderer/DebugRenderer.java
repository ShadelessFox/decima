package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.Renderer;
import com.shade.decima.model.viewer.shader.DebugShaderProgram;
import com.shade.gl.Attribute;
import com.shade.gl.Attribute.ComponentType;
import com.shade.gl.Attribute.Semantic;
import com.shade.gl.VAO;
import com.shade.gl.VBO;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import org.joml.GeometryUtils;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;

public class DebugRenderer implements Renderer {
    private static final Logger log = LoggerFactory.getLogger(DebugRenderer.class);

    private static final List<Attribute> ATTRIBUTES = List.of(
        new Attribute(Semantic.POSITION, ComponentType.FLOAT, 3, 0, 24, false),
        new Attribute(Semantic.COLOR, ComponentType.FLOAT, 3, 12, 24, false)
    );

    private static final int MAX_LINES = 32768;
    private static final int VERTEX_BUFFER_SIZE = 4096;
    private static final int VERTEX_SIZE = 6;

    private DebugShaderProgram program;
    private VAO vao;
    private VBO vbo;

    private final FloatBuffer vertices = BufferUtils.createFloatBuffer(VERTEX_BUFFER_SIZE * VERTEX_SIZE);
    private final Deque<Line> lines = new ArrayDeque<>(MAX_LINES);

    @Override
    public void setup() throws IOException {
        program = new DebugShaderProgram();
        vao = new VAO();

        try (VAO ignored = vao.bind()) {
            vbo = vao.createBuffer(GL_STREAM_DRAW, ATTRIBUTES);
        }

        try (VBO ignored = vbo.bind()) {
            vbo.allocate(vertices.capacity() * Float.BYTES);
        }
    }

    @Override
    public void render(float dt, @NotNull ModelViewport viewport) {
        final Camera camera = viewport.getCamera();

        try (var program = this.program.bind()) {
            program.getView().set(camera.getViewMatrix());
            program.getProjection().set(camera.getProjectionMatrix());

            drawLines();
        }
    }

    @Override
    public void dispose() {
        Disposable.dispose(program);
        Disposable.dispose(vao);

        program = null;
        vao = null;
    }

    public void line(@NotNull Vector3fc from, @NotNull Vector3fc to, @NotNull Vector3fc color, boolean depthTest) {
        if (lines.size() >= MAX_LINES) {
            log.warn("Max lines reached, skipping further line draws");
            return;
        }

        lines.offer(new Line(
            from.x(), from.y(), from.z(),
            to.x(), to.y(), to.z(),
            color.x(), color.y(), color.z(),
            depthTest
        ));
    }

    public void box(@NotNull Vector3fc[] points, @NotNull Vector3fc color, boolean depthTest) {
        for (int i = 0; i < 4; i++) {
            line(points[i], points[(i + 1) % 4], color, depthTest);
            line(points[i + 4], points[(i + 1) % 4 + 4], color, depthTest);
            line(points[i], points[i + 4], color, depthTest);
        }
    }

    public void aabb(@NotNull Vector3fc min, @NotNull Vector3fc max, @NotNull Vector3fc color, boolean depthTest) {
        final Vector3f[] points = {
            new Vector3f(min.x(), min.y(), min.z()),
            new Vector3f(max.x(), min.y(), min.z()),
            new Vector3f(max.x(), max.y(), min.z()),
            new Vector3f(min.x(), max.y(), min.z()),
            new Vector3f(min.x(), min.y(), max.z()),
            new Vector3f(max.x(), min.y(), max.z()),
            new Vector3f(max.x(), max.y(), max.z()),
            new Vector3f(min.x(), max.y(), max.z()),
        };

        box(points, color, depthTest);
    }

    public void cross(@NotNull Vector3fc center, float length, boolean depthTest) {
        final float cx = center.x();
        final float cy = center.y();
        final float cz = center.z();
        final float hl = length * 0.5f;

        final Vector3f src = new Vector3f();
        final Vector3f dst = new Vector3f();

        src.set(cx - hl, cy, cz);
        dst.set(cx + hl, cy, cz);
        line(src, dst, new Vector3f(1, 0, 0), depthTest);

        src.set(cx, cy - hl, cz);
        dst.set(cx, cy + hl, cz);
        line(src, dst, new Vector3f(0, 1, 0), depthTest);

        src.set(cx, cy, cz - hl);
        dst.set(cx, cy, cz + hl);
        line(src, dst, new Vector3f(0, 0, 1), depthTest);
    }

    public void circle(@NotNull Vector3fc center, @NotNull Vector3fc normal, @NotNull Vector3fc color, float radius, int steps, boolean depthTest) {
        final Vector3f up = new Vector3f();
        final Vector3f left = new Vector3f();
        GeometryUtils.perpendicular(normal, left, up);

        left.mul(radius);
        up.mul(radius);

        final Vector3f lastPoint = center.add(up, new Vector3f());

        for (int i = 1; i <= steps; i++) {
            final float radians = (float) (MathUtils.TAU * i / steps);
            final Vector3f vs = left.mul((float) Math.sin(radians), new Vector3f());
            final Vector3f vc = up.mul((float) Math.cos(radians), new Vector3f());
            final Vector3f point = vs.add(vc).add(center);

            line(lastPoint, point, color, depthTest);
            lastPoint.set(point);
        }
    }

    private void drawLines() {
        for (Line line : lines) {
            if (!line.depthTest) {
                push(line, false);
            }
        }

        flushVertices(false);

        for (Line line : lines) {
            if (line.depthTest) {
                push(line, true);
            }
        }

        flushVertices(true);

        lines.clear();
    }

    private void flushVertices(boolean depthTest) {
        if (depthTest) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }

        final int count = vertices.position() / VERTEX_SIZE;

        if (count == 0) {
            return;
        }

        try (VAO ignored = vao.bind()) {
            vbo.put(vertices.flip(), 0);
            glDrawArrays(GL_LINES, 0, count);
        }

        vertices.clear();
    }

    private void push(@NotNull Line value, boolean depthTest) {
        if (vertices.remaining() < VERTEX_SIZE * 2) {
            // Make room for two more vertices
            flushVertices(depthTest);
        }

        push(value.x1, value.y1, value.z1);
        push(value.c1, value.c2, value.c3);
        push(value.x2, value.y2, value.z2);
        push(value.c1, value.c2, value.c3);
    }

    private void push(float x, float y, float z) {
        vertices.put(x).put(y).put(z);
    }

    private record Line(
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float c1, float c2, float c3,
        boolean depthTest
    ) {}
}
