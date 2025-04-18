module decima.ui {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.formdev.flatlaf.extras;
    requires com.formdev.flatlaf;
    requires com.github.weisj.jsvg;
    requires com.google.gson;
    requires com.miglayout.swing;
    requires decima.model;
    requires decima.platform.ui;
    requires decima.platform;
    requires info.picocli;
    requires java.desktop;
    requires java.management;
    requires java.net.http;
    requires java.prefs;
    requires org.slf4j;

    opens com.shade.decima.ui.bookmarks.actions;
    opens com.shade.decima.ui.bookmarks.impl;
    opens com.shade.decima.ui.bookmarks;
    opens com.shade.decima.ui.controls to com.formdev.flatlaf;
    opens com.shade.decima.ui.data.handlers;
    opens com.shade.decima.ui.data.managers;
    opens com.shade.decima.ui.data.viewer.audio.menu;
    opens com.shade.decima.ui.data.viewer.audio.settings;
    opens com.shade.decima.ui.data.viewer.audio;
    opens com.shade.decima.ui.data.viewer.binary;
    opens com.shade.decima.ui.data.viewer.font.menu;
    opens com.shade.decima.ui.data.viewer.font;
    opens com.shade.decima.ui.data.viewer;
    opens com.shade.decima.ui.editor.core.menu.root;
    opens com.shade.decima.ui.editor.core.menu;
    opens com.shade.decima.ui.editor.core.settings;
    opens com.shade.decima.ui.editor.menu;
    opens com.shade.decima.ui.editor;
    opens com.shade.decima.ui.menu.menus;
    opens com.shade.decima.ui.navigator.menu;
    opens com.shade.decima.ui.navigator;
    opens com.shade.decima.ui.updater;
    opens com.shade.decima.ui.views;
    opens com.shade.decima.ui;

    exports com.shade.decima.ui.menu;
    exports com.shade.decima.ui.settings;
    exports com.shade.decima.ui.controls;
    exports com.shade.decima.ui.data;
    exports com.shade.decima.ui.data.registry;
    exports com.shade.decima.ui.editor;
    exports com.shade.decima.ui.editor.core;
    exports com.shade.decima.ui.controls.validators;
    exports com.shade.decima.ui.data.handlers;

    uses com.shade.decima.model.util.hash.spi.Hasher;
    uses com.shade.decima.ui.data.viewer.font.FontExporter;
    uses com.shade.platform.ui.editors.EditorProvider;

    provides com.shade.platform.ui.editors.EditorProvider with
        com.shade.decima.ui.editor.core.CoreEditorProvider,
        com.shade.decima.ui.editor.binary.BinaryEditorProvider;

    provides com.shade.platform.ui.editors.spi.EditorNotificationProvider with
        com.shade.decima.ui.editor.impl.notifications.LoadedWithErrorsEditorNotificationProvider,
        com.shade.decima.ui.editor.impl.notifications.SupersededByPatchEditorNotificationProvider;

    provides com.shade.platform.ui.editors.spi.EditorOnboardingProvider with
        com.shade.decima.ui.editor.impl.DefaultEditorOnboardingProvider;

    provides com.shade.platform.model.app.Application with
        com.shade.decima.ui.Application;

    provides com.shade.decima.ui.data.viewer.font.FontExporter with
        com.shade.decima.ui.data.viewer.font.exporter.FontExporterSVG;
}