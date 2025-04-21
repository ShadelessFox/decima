module decima.opengl {
    requires decima.platform;
    requires org.joml;
    requires org.lwjgl.jawt;
    requires org.lwjgl.natives;
    requires org.lwjgl.opengl.natives;
    requires org.lwjgl.opengl;
    requires platform.util;

    exports com.shade.gl;
    exports com.shade.gl.awt;
}
