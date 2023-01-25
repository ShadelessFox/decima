package com.shade.decima.ui.data.viewer.model.model;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializer;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public class ModelViewerPanel extends JComponent {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(List.class, (JsonSerializer<List<?>>) (src, type, context) -> {
            if (src == null || src.isEmpty()) {
                return null;
            }
            final JsonArray result = new JsonArray();
            for (Object o : src) {
                result.add(context.serialize(o));
            }
            return result;
        })
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();


    private final JButton exportButton;
    private final JCheckBox exportTextures;
    private final JCheckBox embeddedTexturesCheckBox;
    private final JComboBox<ModelExporterProvider> exportersCombo;
    private CoreEditor editor;

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
            chooser.setSelectedFile(new File(IOUtils.getBasename(editor.getInput().getName()) + "." + provider.getExtension()));
            chooser.setDialogTitle("Choose output file");
            chooser.setFileFilter(new FileExtensionFilter(provider.getName(), provider.getExtension()));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            ProgressDialog.showProgressDialog(Application.getFrame(), "Export models", monitor -> {
                try {
                    export(monitor, chooser.getSelectedFile().toPath());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                return null;
            });


            JOptionPane.showMessageDialog(Application.getFrame(), "Done");
        });

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new LabeledBorder(new JLabel("Export settings")));
        settingsPanel.add(exportTextures = new JCheckBox("Export textures", false));
        settingsPanel.add(embeddedTexturesCheckBox = new JCheckBox("Embed textures", true));

        setLayout(new MigLayout("ins panel", "[grow,fill]", "[grow,fill][][][]"));
        add(placeholder, "wrap");
        add(exportersCombo, "wrap");
        add(settingsPanel, "wrap");
        add(exportButton);
    }

    public void setInput(@Nullable CoreEditor editor) {
        this.editor = editor;
        this.exportButton.setEnabled(editor != null);
    }

    private void export(@NotNull ProgressMonitor monitor, @NotNull Path output) throws Throwable {
        final var provider = exportersCombo.getItemAt(exportersCombo.getSelectedIndex());
        final var object = (RTTIObject) Objects.requireNonNull(editor.getSelectedValue());

        final var settings = new ExportSettings(exportTextures.isSelected(), embeddedTexturesCheckBox.isSelected());
        final var exporter = provider.create(editor.getInput().getProject(), settings, output.getParent());
        final var name = IOUtils.getBasename(output.getFileName().toString());

        try (ProgressMonitor.Task task = monitor.begin("Exporting %s".formatted(name), 2)) {
            Object result = exporter.export(task.split(1), editor.getCoreBinary(), object, name);
            Files.writeString(output, GSON.toJson(result));
        }
    }
}
