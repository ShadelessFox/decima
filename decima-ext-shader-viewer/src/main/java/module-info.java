module decima.ui.viewer.shader {
    requires com.formdev.flatlaf;
    requires com.miglayout.swing;
    requires decima.model;
    requires decima.platform.ui;
    requires decima.platform;
    requires decima.ui;
    requires java.desktop;

    opens com.shade.decima.ui.data.viewer.shader.settings;
    opens com.shade.decima.ui.data.viewer.shader;
}
