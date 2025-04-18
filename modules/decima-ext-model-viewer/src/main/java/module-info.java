module decima.ui.viewer.model {
    requires lwjgl3.awt;
    requires decima.opengl;
    requires decima.ui;
    requires decima.model;
    requires decima.platform.ui;
    requires decima.platform;
    requires com.formdev.flatlaf;
    requires org.slf4j;
    requires com.miglayout.swing;
    requires java.desktop;

    opens com.shade.decima.model.viewer.menu;
    opens com.shade.decima.model.viewer.outline.menu;
    opens com.shade.decima.model.viewer.settings;
    opens com.shade.decima.model.viewer;

    exports com.shade.decima.model.viewer.scene;
    exports com.shade.decima.model.viewer;
}