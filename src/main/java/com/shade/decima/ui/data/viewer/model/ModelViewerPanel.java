package com.shade.decima.ui.data.viewer.model;

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
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public class ModelViewerPanel extends JComponent {
    private static final Logger log = LoggerFactory.getLogger(ModelViewerPanel.class);
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
    private final JCheckBox embeddedBuffersCheckBox;
    private final JComboBox<ModelExporterProvider> exportersCombobox;
    private CoreEditor editor;

    public ModelViewerPanel() {
        final JLabel placeholder = new JLabel("Preview is not supported");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

        final ModelExporterProvider[] modelExporterProviders = ServiceLoader.load(ModelExporterProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(ModelExporterProvider[]::new);

        exportersCombobox = new JComboBox<>(modelExporterProviders);
        exportersCombobox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ModelExporterProvider> list, @NotNull ModelExporterProvider value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
        exportButton = new JButton("Export\u2026");
        exportButton.setEnabled(false);
        exportButton.addActionListener(event -> {
            final ModelExporterProvider provider = exportersCombobox.getItemAt(exportersCombobox.getSelectedIndex());
            final JFileChooser chooser = new JFileChooser();
            String name = editor.getInput().getName();
            if (name.indexOf('.') >= 0)
                name = name.substring(0, name.lastIndexOf('.'));
            chooser.setSelectedFile(new File(name + "." + provider.getExtension()));
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
        settingsPanel.setBorder(new LabeledBorder("Export settings"));
        settingsPanel.add(exportTextures = new JCheckBox("Export textures", false));
        settingsPanel.add(embeddedBuffersCheckBox = new JCheckBox("Embed buffers", true));
        settingsPanel.add(embeddedTexturesCheckBox = new JCheckBox("Embed textures", true));

        setLayout(new MigLayout("ins panel", "[grow,fill]", "[grow,fill][][][]"));
        add(placeholder, "wrap");
        add(exportersCombobox, "wrap");
        add(settingsPanel, "wrap");
        add(exportButton);
    }

    public void setInput(@Nullable CoreEditor editor) {
        this.editor = editor;
        this.exportButton.setEnabled(editor != null);
    }

    private void export(@NotNull ProgressMonitor monitor, @NotNull Path output) throws Throwable {
        final ModelExporterProvider provider = exportersCombobox.getItemAt(exportersCombobox.getSelectedIndex());
        final var object = (RTTIObject) Objects.requireNonNull(editor.getSelectedValue());
        String filename = output.getFileName().toString();


        String resourceName = filename.substring(0, filename.indexOf('.'));
        ExportSettings exportSettings = new ExportSettings(exportTextures.isSelected(), embeddedBuffersCheckBox.isSelected(), embeddedTexturesCheckBox.isSelected());
        ModelExporter exporter = provider.create(editor.getInput().getProject(), exportSettings, output.getParent());
        try (ProgressMonitor.Task task = monitor.begin("Exporting %s".formatted(resourceName), 2)) {
            Object result = exporter.export(task.split(1), editor.getCoreBinary(), object, resourceName);
            Files.writeString(output, GSON.toJson(result));
        }


    }


}
