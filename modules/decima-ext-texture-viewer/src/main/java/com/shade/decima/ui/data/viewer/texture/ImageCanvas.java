package com.shade.decima.ui.data.viewer.texture;

import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.gl.Attribute;
import com.shade.gl.Texture;
import com.shade.gl.VAO;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;

public class ImageCanvas extends AWTGLCanvas implements Disposable {
    private static final Attribute[] ATTRIBUTES = {
        new Attribute(Attribute.Semantic.POSITION, Attribute.ComponentType.FLOAT, 2, 0, 16, false),
        new Attribute(Attribute.Semantic.TEXTURE, Attribute.ComponentType.FLOAT, 2, 8, 16, false)
    };

    private static final int FLAG_SHOW_GRID = 1;
    private static final int FLAG_R         = 1 << 1;
    private static final int FLAG_G         = 1 << 2;
    private static final int FLAG_B         = 1 << 3;
    private static final int FLAG_A         = 1 << 4;
    private static final int FLAG_RGB       = FLAG_R | FLAG_G | FLAG_B;
    private static final int FLAG_RGBA      = FLAG_R | FLAG_G | FLAG_B | FLAG_A;

    private static final float[] DATA = {
        // position   // texture
        +1.0f, +1.0f, 1.0f, 0.0f, // top right
        +1.0f, -1.0f, 1.0f, 1.0f, // bottom right
        -1.0f, -1.0f, 0.0f, 1.0f, // bottom left
        -1.0f, +1.0f, 0.0f, 0.0f // top left
    };

    private ImageProvider provider;
    private boolean dirty;

    private ImageShaderProgram program;
    private VAO vao;
    private Texture texture;

    public ImageCanvas() {
        super(createData());
    }

    @Override
    public void initGL() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        try {
            program = new ImageShaderProgram();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        vao = new VAO();
        vao.createBuffer(ATTRIBUTES).put(DATA);
        vao.createIndexBuffer().put(new int[]{0, 1, 3, 1, 2, 3});
    }

    @Override
    public void paintGL() {
        final double scaleFactor = UIScale.getSystemScaleFactor(getGraphicsConfiguration());
        final int width = (int) (getWidth() * scaleFactor);
        final int height = (int) (getHeight() * scaleFactor);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);

        if (dirty) {
            if (provider != null) {
                texture = createTexture(provider);
            } else {
                texture = null;
            }

            dirty = false;
        }

        if (texture != null) {
            glActiveTexture(GL_TEXTURE0);

            try (var vao = this.vao.bind();
                 var program = (ImageShaderProgram) this.program.bind();
                 var texture = this.texture.bind()
            ) {
                final Point point = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(point, this);

                program.getSampler().set(0);
                program.getViewport().set(new Vector2f(getWidth(), getHeight()));
                program.getSize().set(new Vector2f(provider.getWidth(), provider.getHeight()));
                program.getLocation().set(new Vector2f(point.x, point.y));
                program.getZoom().set(1.0f);
                program.getGamma().set(1.0f);
                program.getExposure().set(1.0f);
                program.getFlags().set(FLAG_RGB/* | FLAG_SHOW_GRID*/);

                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            }
        }

        swapBuffers();
    }

    @Override
    public void dispose() {
        Disposable.dispose(program);
        Disposable.dispose(vao);
        Disposable.dispose(texture);

        program = null;
        vao = null;
        texture = null;
    }

    public void setProvider(@Nullable ImageProvider provider) {
        this.provider = provider;
        this.dirty = true;
    }

    @NotNull
    private static GLData createData() {
        final GLData data = new GLData();
        data.majorVersion = 4;
        data.minorVersion = 3;
        data.profile = GLData.Profile.CORE;
        return data;
    }

    @Nullable
    private static Texture createTexture(@NotNull ImageProvider provider) {
        final ByteBuffer data = provider.getData(0, 0);
        final int width = provider.getWidth();
        final int height = provider.getHeight();

        return switch (provider.getPixelFormat()) {
            case "BC1" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGB_S3TC_DXT1_EXT);
            case "BC2" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGBA_S3TC_DXT3_EXT);
            case "BC3" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT);
            case "BC4U" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RED_RGTC1);
            case "BC4S" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_SIGNED_RED_RGTC1);
            case "BC5U" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RG_RGTC2);
            case "BC5S" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_SIGNED_RG_RGTC2);
            case "BC6U" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT);
            case "BC6S" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT);
            case "BC7" -> Texture.fromCompressed2D(data, width, height, GL_COMPRESSED_RGBA_BPTC_UNORM);
            case "RGBA_8888" -> Texture.from2D(data, width, height, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE);
            case "R_UNORM_8" -> Texture.from2D(data, width, height, GL_RED, GL_RED, GL_UNSIGNED_BYTE);
            case "RGBA_FLOAT_16" -> Texture.from2D(data, width, height, GL_RGBA, GL_RGBA, GL_HALF_FLOAT);
            default -> {
                System.out.println("Unsupported pixel format: " + provider.getPixelFormat());
                yield null;
            }
        };
    }
}
