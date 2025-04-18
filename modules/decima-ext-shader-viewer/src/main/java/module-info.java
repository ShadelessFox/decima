module decima.ui.viewer.shader {
    requires decima.ui;
    requires decima.platform.ui;
    requires com.miglayout.swing;
    requires com.formdev.flatlaf;
    requires decima.model;
    requires decima.platform;
    requires java.desktop;
    requires com.sun.jna;

    opens com.shade.decima.ui.data.viewer.shader.settings;
    opens com.shade.decima.ui.data.viewer.shader;
}