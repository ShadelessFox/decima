module decima.ui.viewer.model {
    requires com.formdev.flatlaf;
    requires com.miglayout.swing;
    requires decima.model;
    requires decima.opengl;
    requires decima.platform.ui;
    requires decima.platform;
    requires decima.ui;
    requires java.desktop;
    requires org.joml;
    requires org.lwjgl.opengl;
    requires org.slf4j;
    requires platform.util;

    opens com.shade.decima.model.viewer.menu;
    opens com.shade.decima.model.viewer.outline.menu;
    opens com.shade.decima.model.viewer.settings;
    opens com.shade.decima.model.viewer;

    exports com.shade.decima.model.viewer.scene;
    exports com.shade.decima.model.viewer;
}
