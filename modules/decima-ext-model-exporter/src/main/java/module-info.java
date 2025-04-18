module decima.ui.exporter.model {
    requires decima.ui.viewer.model;
    requires decima.ui.viewer.texture;
    requires lwjgl3.awt;
    requires decima.ui;
    requires decima.model;
    requires decima.opengl;
    requires decima.platform.ui;
    requires decima.platform;
    requires com.google.gson;
    requires org.slf4j;
    requires com.miglayout.swing;
    requires com.formdev.flatlaf;
    requires java.desktop;

    uses com.shade.decima.ui.data.viewer.model.ModelExporterProvider;

    provides com.shade.decima.ui.data.viewer.model.ModelExporterProvider with
        com.shade.decima.ui.data.viewer.model.dmf.DMFExporter.Provider,
        com.shade.decima.ui.data.viewer.model.gltf.GltfExporter.GltfProvider,
        com.shade.decima.ui.data.viewer.model.gltf.GltfExporter.GlbProvider;
}