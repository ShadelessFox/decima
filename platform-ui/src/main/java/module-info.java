module decima.platform.ui {
    requires com.formdev.flatlaf;
    requires com.miglayout.swing;
    requires decima.platform;
    requires java.desktop;
    requires java.management;
    requires java.prefs;
    requires org.slf4j;
    requires platform.util;

    exports com.shade.platform.ui.commands;
    exports com.shade.platform.ui.controls.plaf;
    exports com.shade.platform.ui.controls.tree;
    exports com.shade.platform.ui.controls.validation;
    exports com.shade.platform.ui.controls;
    exports com.shade.platform.ui.dialogs;
    exports com.shade.platform.ui.editors.lazy;
    exports com.shade.platform.ui.editors.menu;
    exports com.shade.platform.ui.editors.spi;
    exports com.shade.platform.ui.editors.stack;
    exports com.shade.platform.ui.editors;
    exports com.shade.platform.ui.icons;
    exports com.shade.platform.ui.menus;
    exports com.shade.platform.ui.settings.impl;
    exports com.shade.platform.ui.settings;
    exports com.shade.platform.ui.util;
    exports com.shade.platform.ui.views;
    exports com.shade.platform.ui.wm;
    exports com.shade.platform.ui;

    opens com.shade.platform.ui.controls.plaf to java.desktop;
    opens com.shade.platform.ui.editors.menu;
    opens com.shade.platform.ui.menus.impl;

    uses com.shade.platform.ui.editors.EditorProvider;
    uses com.shade.platform.ui.editors.spi.EditorNotificationProvider;
    uses com.shade.platform.ui.editors.spi.EditorOnboardingProvider;

    provides com.shade.platform.ui.editors.EditorProvider
        with com.shade.platform.ui.editors.lazy.LazyEditorProvider;
}
