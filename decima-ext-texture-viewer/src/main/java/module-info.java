module decima.ui.viewer.texture {
    requires be.twofold.tinybcdec;
    requires com.formdev.flatlaf;
    requires com.miglayout.swing;
    requires decima.model;
    requires decima.platform.ui;
    requires decima.platform;
    requires decima.ui;
    requires java.desktop;
    requires org.slf4j;
    requires platform.util;

    opens com.shade.decima.ui.data.viewer.texture.menu;
    opens com.shade.decima.ui.data.viewer.texture.settings;
    opens com.shade.decima.ui.data.viewer.texture;

    exports com.shade.decima.ui.data.viewer.texture;
    exports com.shade.decima.ui.data.viewer.texture.controls;
    exports com.shade.decima.ui.data.viewer.texture.exporter;

    uses com.shade.decima.ui.data.viewer.texture.TextureExporter;
    uses com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;

    provides com.shade.decima.ui.data.viewer.texture.TextureExporter with
        com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterDDS,
        com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterPNG,
        com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterTIFF;

    provides com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider with
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC1.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC2.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC3.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC4.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC5.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC6.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderBC7.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderRGBA8.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderRGBA16F.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderR8.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderR16F.Provider,
        com.shade.decima.ui.data.viewer.texture.reader.ImageReaderR16.Provider;
}
