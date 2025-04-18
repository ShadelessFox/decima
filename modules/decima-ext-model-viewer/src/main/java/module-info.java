module decima.ui.viewer.model {
    requires decima.platform.ui;
    requires lwjgl3.awt;
    requires decima.opengl;
    requires decima.platform;
    requires com.formdev.flatlaf;
    requires org.slf4j;
    requires com.miglayout.swing;
    requires java.desktop;

    opens com.shade.decima.model.viewer.settings;
    opens com.shade.decima.model.viewer;

    exports com.shade.decima.model.viewer.scene;
}