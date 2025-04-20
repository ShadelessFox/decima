package com.shade.decima.ui.updater;

import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.HyperlinkAdapter;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

@SettingsPageRegistration(parent = "application", id = "updates", name = "Updates")
public class UpdateSettingsPage implements SettingsPage {
    private final Set<String> ignoredUpdates = new LinkedHashSet<>();

    private JCheckBox checkForUpdatesCheck;
    private JLabel lastUpdateCheckLabel;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins panel,flowx"));
        panel.add(checkForUpdatesCheck = new JCheckBox("Check for application updates"));
        panel.add(UIUtils.createHelpToolTip("Check is performed every 24 hours"), "wrap");

        JButton checkButton = new JButton("Check for updates now\u2026");
        checkButton.addActionListener(e -> {
            UpdateService.getInstance().checkForUpdatesModal(JOptionPane.getRootFrame());
            updateLastChecked();
        });

        panel.add(checkButton);
        panel.add(lastUpdateCheckLabel = new JLabel(), "wrap");

        panel.add(UIUtils.createText("<a href=''>Manage ignored updates\u2026</a>", new HyperlinkAdapter() {
            @Override
            public void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                JTextArea area = new JTextArea(10, 40);
                area.setText(String.join(System.lineSeparator(), ignoredUpdates));

                int result = JOptionPane.showConfirmDialog(
                    JOptionPane.getRootFrame(),
                    new JScrollPane(area),
                    "Ignored updates",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );

                if (result == JOptionPane.OK_OPTION) {
                    ignoredUpdates.clear();
                    ignoredUpdates.addAll(area.getText()
                        .lines()
                        .map(String::strip)
                        .filter(Predicate.not(String::isEmpty))
                        .toList());
                }
            }
        }), "growx");

        return panel;
    }

    @Override
    public void apply() {
        UpdateService.Settings settings = UpdateService.getInstance().getSettings();
        settings.checkForUpdates = checkForUpdatesCheck.isSelected();
        settings.ignoredUpdates.clear();
        settings.ignoredUpdates.addAll(ignoredUpdates);
    }

    @Override
    public void reset() {
        UpdateService.Settings settings = UpdateService.getInstance().getSettings();
        checkForUpdatesCheck.setSelected(settings.checkForUpdates);
        ignoredUpdates.clear();
        ignoredUpdates.addAll(settings.ignoredUpdates);
        updateLastChecked();
    }

    @Override
    public boolean isModified() {
        UpdateService.Settings settings = UpdateService.getInstance().getSettings();
        return settings.checkForUpdates != checkForUpdatesCheck.isSelected()
            || !settings.ignoredUpdates.equals(ignoredUpdates);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    private void updateLastChecked() {
        LocalDateTime lastCheck = UpdateService.getInstance().getSettings().lastCheck;
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        lastUpdateCheckLabel.setText("Last checked: " + (lastCheck != null ? formatter.format(lastCheck) : "N/A"));
    }
}
