package com.shade.decima.ui.data.viewer.model;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatHelpButtonIcon;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

public class ModelViewerPanel extends JComponent {
    private final JButton exportButton;
    private final JCheckBox embeddedBuffersCheckBox;
    private final JCheckBox exportTextures;
    private final JCheckBox embeddedTexturesCheckBox;
    private final JCheckBox useInstancingCheckBox;
    private final JCheckBox exportLodsCheckBox;
    private final JComboBox<ModelExporterProvider> exportersCombo;
    private ValueController<?> controller;

    public ModelViewerPanel() {
        final JLabel placeholder = new JLabel("Preview is not supported");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

        final ModelExporterProvider[] modelExporterProviders = ServiceLoader.load(ModelExporterProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(ModelExporterProvider[]::new);

        exportersCombo = new JComboBox<>(modelExporterProviders);
        exportersCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ModelExporterProvider> list, @NotNull ModelExporterProvider value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
        exportButton = new JButton("Export\u2026");
        exportButton.setEnabled(false);
        exportButton.addActionListener(event -> {
            final ModelExporterProvider provider = exportersCombo.getItemAt(exportersCombo.getSelectedIndex());
            final JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(IOUtils.getBasename(controller.getEditor().getInput().getName()) + "." + provider.getExtension()));
            chooser.setDialogTitle("Choose output file");
            chooser.setFileFilter(new FileExtensionFilter(provider.getName(), provider.getExtension()));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(Application.getInstance().getFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final Path output = chooser.getSelectedFile().toPath();
            final Boolean done = ProgressDialog.showProgressDialog(Application.getInstance().getFrame(), "Export models", monitor -> {
                try {
                    export(monitor, output);
                    return true;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null);

            if (done == Boolean.TRUE) {
                JOptionPane.showMessageDialog(Application.getInstance().getFrame(), "Done");
            } else {
                IOUtils.unchecked(() -> Files.deleteIfExists(output));
            }
        });

        exportLodsCheckBox = new JCheckBox("Export LODs", false);
        useInstancingCheckBox = new JCheckBox("Use mesh instancing", true);
        embeddedBuffersCheckBox = new JCheckBox("Embed buffers", true);
        embeddedTexturesCheckBox = new JCheckBox("Embed textures", true);
        embeddedTexturesCheckBox.setEnabled(false);

        exportTextures = new JCheckBox("Export textures", false);
        exportTextures.addItemListener(e -> embeddedTexturesCheckBox.setEnabled(exportTextures.isSelected()));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new MigLayout("ins panel,gap 0", "[grow,fill]"));
        settingsPanel.setBorder(new LabeledBorder("Options"));
        settingsPanel.add(exportLodsCheckBox, "wrap");
        settingsPanel.add(useInstancingCheckBox, "wrap");
        settingsPanel.add(exportTextures, "wrap");
        settingsPanel.add(embeddedTexturesCheckBox, "wrap");
        settingsPanel.add(embeddedBuffersCheckBox, "wrap");

        final JToolBar toolBar = new JToolBar();
        toolBar.setBorder(null);
        toolBar.add(new AbstractAction(null, new FlatHelpButtonIcon()) {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    Desktop.getDesktop().browse(URI.create("https://github.com/ShadelessFox/decima/wiki/Model-export"));
                } catch (IOException e) {
                    UIUtils.showErrorDialog(Application.getInstance().getFrame(), e, "Unable to open wiki page");
                }
            }
        });

        setLayout(new MigLayout("ins panel,gap 0", "[grow,fill]", "[grow,fill][][][]"));
        add(placeholder, "wrap");
        add(new JLabel("Output format:"), "wrap");
        add(exportersCombo, "grow x,split");
        add(toolBar, "gapx 0,wrap");
        add(settingsPanel, "wrap");
        add(exportButton);
    }

    public void setInput(@Nullable ValueController<?> controller) {
        this.controller = controller;
        this.exportButton.setEnabled(controller != null);
    }

    private void export(@NotNull ProgressMonitor monitor, @NotNull Path output) throws Throwable {
        final var provider = exportersCombo.getItemAt(exportersCombo.getSelectedIndex());
        final var object = (RTTIObject) controller.getValue();

        final var settings = new ExportSettings(exportTextures.isSelected(), embeddedTexturesCheckBox.isSelected(), exportLodsCheckBox.isSelected(), useInstancingCheckBox.isSelected(), embeddedBuffersCheckBox.isSelected());
        final var exporter = provider.create(controller.getProject(), settings, output.getParent());
        final var name = IOUtils.getBasename(output.getFileName().toString());

        try (ProgressMonitor.Task task = monitor.begin("Exporting %s".formatted(name), 2)) {
            try (Writer writer = Files.newBufferedWriter(output)) {
                exporter.export(task.split(1), ((CoreEditor) controller.getEditor()).getBinary(), object, name, writer);
            }
        }
    }
}
