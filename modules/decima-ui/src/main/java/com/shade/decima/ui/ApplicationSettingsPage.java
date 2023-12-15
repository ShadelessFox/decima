package com.shade.decima.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

@SettingsPageRegistration(id = "application", name = "Application")
public class ApplicationSettingsPage implements SettingsPage {
    private JComboBox<ThemeInfo> themeCombo;
    private JComboBox<FontInfo> fontFamilyCombo;
    private JLabel fontSizeLabel;
    private JSpinner fontSizeSpinner;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Appearance"));
        panel.setLayout(new MigLayout("ins panel"));

        panel.add(new JLabel("Theme:"));
        panel.add(themeCombo = new JComboBox<>(ThemeInfo.getAvailableThemes()), "wrap");

        panel.add(new JLabel("Font:"));
        panel.add(fontFamilyCombo = new JComboBox<>(FontInfo.getAvailableFonts()));
        panel.add(fontSizeLabel = new JLabel("Size:"));
        panel.add(fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 6, 72, 1)), "wrap");

        fontFamilyCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends FontInfo> list, FontInfo value, int index, boolean selected, boolean focused) {
                append(value.fontFamily, TextAttributes.REGULAR_ATTRIBUTES);
            }

            @Nullable
            @Override
            protected String getTitle(@NotNull JList<? extends FontInfo> list, FontInfo value, int index) {
                return switch (index) {
                    case 0 -> "Default font";
                    case 1 -> "Available fonts";
                    default -> null;
                };
            }
        });

        fontFamilyCombo.addItemListener(e -> {
            final boolean custom = !fontFamilyCombo.getItemAt(fontFamilyCombo.getSelectedIndex()).lafDefault;
            fontSizeLabel.setEnabled(custom);
            fontSizeSpinner.setEnabled(custom);
        });

        // FIXME Not fancy
        final Runnable adapter = () -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        themeCombo.addItemListener(e -> adapter.run());
        fontFamilyCombo.addItemListener(e -> adapter.run());
        fontSizeSpinner.addChangeListener(e -> adapter.run());

        return panel;
    }

    @Override
    public void apply() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();

        if (isFontChanged(settings)) {
            final FontInfo info = fontFamilyCombo.getItemAt(fontFamilyCombo.getSelectedIndex());

            if (info.lafDefault) {
                settings.customFontFamily = null;
                settings.customFontSize = 0;
            } else {
                settings.customFontFamily = info.fontFamily;
                settings.customFontSize = (int) fontSizeSpinner.getValue();
            }

            MessageBus.getInstance().publisher(ApplicationSettings.SETTINGS).fontChanged(settings.customFontFamily, settings.customFontSize);
        }

        if (isThemeChanged(settings)) {
            settings.themeClassName = themeCombo.getItemAt(themeCombo.getSelectedIndex()).className;

            MessageBus.getInstance().publisher(ApplicationSettings.SETTINGS).themeChanged(settings.themeClassName);
        }
    }

    @Override
    public void reset() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();

        for (ThemeInfo theme : ThemeInfo.getAvailableThemes()) {
            if (theme.className.equals(settings.themeClassName)) {
                themeCombo.setSelectedItem(theme);
                break;
            }
        }

        if (settings.customFontFamily == null) {
            fontFamilyCombo.setSelectedItem(FontInfo.DEFAULT_FONT);
            fontSizeSpinner.setValue(UIManager.getFont("defaultFont").getSize());
        } else {
            fontFamilyCombo.setSelectedItem(new FontInfo(settings.customFontFamily, false));
            fontSizeSpinner.setValue(settings.customFontSize);
        }
    }

    @Override
    public boolean isModified() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();
        return isFontChanged(settings) || isThemeChanged(settings);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    private boolean isThemeChanged(@NotNull ApplicationSettings settings) {
        final ThemeInfo info = themeCombo.getItemAt(themeCombo.getSelectedIndex());
        return !info.className.equals(settings.themeClassName);
    }

    private boolean isFontChanged(@NotNull ApplicationSettings settings) {
        final FontInfo info = fontFamilyCombo.getItemAt(fontFamilyCombo.getSelectedIndex());
        return settings.customFontFamily == null == !info.lafDefault
            || settings.customFontFamily != null && !settings.customFontFamily.equals(info.fontFamily)
            || settings.customFontFamily != null && settings.customFontSize != (int) fontSizeSpinner.getValue();
    }

    private record ThemeInfo(@NotNull String name, @NotNull String className) {
        private static final ThemeInfo[] THEMES = {
            new ThemeInfo("Light", FlatLightLaf.class.getName()),
            new ThemeInfo("Dark", FlatDarkLaf.class.getName())
        };

        @NotNull
        public static ThemeInfo[] getAvailableThemes() {
            return THEMES;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private record FontInfo(@NotNull String fontFamily, boolean lafDefault) {
        public static final FontInfo DEFAULT_FONT = new FontInfo("Default font", true);

        @NotNull
        public static FontInfo[] getAvailableFonts() {
            final List<FontInfo> fonts = new ArrayList<>();

            // Default font
            fonts.add(DEFAULT_FONT);

            // Available fonts
            for (String font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                fonts.add(new FontInfo(font, false));
            }

            return fonts.toArray(FontInfo[]::new);
        }

        @Override
        public String toString() {
            return fontFamily;
        }
    }
}
