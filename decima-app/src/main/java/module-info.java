module decima.app {
    requires com.miglayout.swing;
    requires decima.core;
    requires decima.game.hfw;
    requires decima.opengl;
    requires decima.platform.ui;
    requires decima.platform;
    requires decima.rtti;
    requires decima.ui.exporter.model;
    requires decima.ui.viewer.model;
    requires decima.ui.viewer.shader;
    requires decima.ui.viewer.texture;
    requires java.desktop;
    requires org.lwjgl.opengl;
    requires org.slf4j;

    opens com.shade.decima.app.ui.util;
}
