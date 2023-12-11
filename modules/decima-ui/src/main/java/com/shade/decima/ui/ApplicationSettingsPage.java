package com.shade.decima.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.model.messages.MessageBus;
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
import java.util.Objects;

@SettingsPageRegistration(id = "application", name = "Application")
public class ApplicationSettingsPage implements SettingsPage {
    private static final ThemeInfo[] THEMES = {
        new ThemeInfo("Light", FlatLightLaf.class.getName()),
        new ThemeInfo("Dark", FlatDarkLaf.class.getName())
    };

    private JComboBox<ThemeInfo> themeCombo;

    private JCheckBox useCustomFontCheckbox;
    private JComboBox<String> fontCombo;
    private JLabel fontSizeLabel;
    private JSpinner fontSizeSpinner;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Appearance"));
        panel.setLayout(new MigLayout("ins panel"));

        panel.add(new JLabel("Theme:"));
        panel.add(themeCombo = new JComboBox<>(THEMES), "wrap");

        panel.add(useCustomFontCheckbox = new JCheckBox("Use custom font:", true));
        panel.add(fontCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

        panel.add(fontSizeLabel = new JLabel("Size:"));
        panel.add(fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 6, 72, 1)), "wrap");

        useCustomFontCheckbox.addItemListener(e -> {
            final boolean selected = useCustomFontCheckbox.isSelected();
            fontSizeLabel.setEnabled(selected);
            fontCombo.setEnabled(selected);
            fontSizeSpinner.setEnabled(selected);
        });

        // FIXME Not fancy
        final Runnable adapter = () -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        themeCombo.addItemListener(e -> adapter.run());
        useCustomFontCheckbox.addItemListener(e -> adapter.run());
        fontCombo.addItemListener(e -> adapter.run());
        fontSizeSpinner.addChangeListener(e -> adapter.run());

        return panel;
    }

    @Override
    public void apply() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();

        if (isFontChanged(settings)) {
            if (useCustomFontCheckbox.isSelected()) {
                settings.customFontFamily = fontCombo.getItemAt(fontCombo.getSelectedIndex());
                settings.customFontSize = (int) fontSizeSpinner.getValue();
            } else {
                settings.customFontFamily = null;
                settings.customFontSize = 0;
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

        themeCombo.setSelectedItem(findTheme(settings.themeClassName));
        useCustomFontCheckbox.setSelected(settings.customFontFamily != null);

        if (settings.customFontFamily == null) {
            final Font defaultFont = UIManager.getFont("defaultFont");
            fontCombo.setSelectedItem(defaultFont.getFamily());
            fontSizeSpinner.setValue(defaultFont.getSize());
        } else {
            fontCombo.setSelectedItem(settings.customFontFamily);
            fontSizeSpinner.setValue(settings.customFontSize);
        }
    }

    @Override
    public boolean isModified() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();

        return isFontChanged(settings)
            || isThemeChanged(settings);
    }

    private boolean isThemeChanged(ApplicationSettings settings) {
        return !Objects.equals(settings.themeClassName, themeCombo.getItemAt(themeCombo.getSelectedIndex()).className);
    }

    private boolean isFontChanged(@NotNull ApplicationSettings settings) {
        return settings.customFontFamily == null == useCustomFontCheckbox.isSelected()
            || settings.customFontFamily != null && !settings.customFontFamily.equals(fontCombo.getSelectedItem())
            || settings.customFontFamily != null && settings.customFontSize != (int) fontSizeSpinner.getValue();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @NotNull
    private ThemeInfo findTheme(@Nullable String className) {
        if (className != null) {
            for (ThemeInfo theme : THEMES) {
                if (theme.className.equals(className)) {
                    return theme;
                }
            }
        }

        return THEMES[0];
    }

    private record ThemeInfo(@NotNull String name, @NotNull String className) {
        @Override
        public String toString() {
            return name;
        }
    }
}
