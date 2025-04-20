module decima.ui.exporter.model {
    requires com.formdev.flatlaf;
    requires com.google.gson;
    requires com.miglayout.swing;
    requires decima.model;
    requires decima.opengl;
    requires decima.platform.ui;
    requires decima.platform;
    requires decima.ui.viewer.model;
    requires decima.ui.viewer.texture;
    requires decima.ui;
    requires java.desktop;
    requires org.joml;
    requires org.slf4j;
    requires platform.util;

    opens com.shade.decima.ui.data.viewer.model.menu;

    uses com.shade.decima.ui.data.viewer.model.ModelExporterProvider;

    provides com.shade.decima.ui.data.viewer.model.ModelExporterProvider with
        com.shade.decima.ui.data.viewer.model.dmf.DMFExporter.Provider,
        com.shade.decima.ui.data.viewer.model.gltf.GltfExporter.GltfProvider,
        com.shade.decima.ui.data.viewer.model.gltf.GltfExporter.GlbProvider;
}
