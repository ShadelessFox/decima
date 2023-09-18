package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;

public class ModelExportDialog extends BaseDialog {
    private static final DataKey<ModelExporterProvider.Option> OPTION_KEY = new DataKey<>("option", ModelExporterProvider.Option.class);

    private final ValueController<RTTIObject> controller;

    private final JComboBox<ModelExporterProvider> exporterCombo;
    private final List<JCheckBox> optionCheckboxes;

    public ModelExportDialog(@NotNull ValueController<RTTIObject> controller) {
        super("Export Model Settings");
        this.controller = controller;

        this.optionCheckboxes = new ArrayList<>();

        for (ModelExporterProvider.Option option : ModelExporterProvider.Option.values()) {
            final JCheckBox checkbox = new JCheckBox(option.getLabel(), option.isEnabledByDefault());
            checkbox.putClientProperty(OPTION_KEY, option);
            optionCheckboxes.add(checkbox);
        }

        final ModelExporterProvider[] exporters = ServiceLoader.load(ModelExporterProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(ModelExporterProvider[]::new);

        this.exporterCombo = new JComboBox<>(exporters);
        this.exporterCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ModelExporterProvider> list, @NotNull ModelExporterProvider value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
        this.exporterCombo.addItemListener(e -> {
            final ModelExporterProvider exporter = (ModelExporterProvider) e.getItem();
            for (JCheckBox checkbox : optionCheckboxes) {
                final ModelExporterProvider.Option option = OPTION_KEY.get(checkbox);
                checkbox.setEnabled(exporter.supportsOption(option));
            }
        });

        // HACK: Force controls update
        exporterCombo.setSelectedIndex(-1);
        exporterCombo.setSelectedIndex(0);
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel options = new JPanel();
        options.setLayout(new MigLayout("ins panel,gap 0", "[grow,fill,250lp]", ""));
        options.setBorder(new LabeledBorder("Options"));

        for (JCheckBox checkbox : optionCheckboxes) {
            options.add(checkbox, "wrap");
        }

        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0", "[grow,fill]", "[][][grow,fill]"));
        panel.add(new JLabel("Output format:"), "wrap");
        panel.add(exporterCombo, "wrap");
        panel.add(options);

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_SAVE) {
            final ModelExporterProvider provider = exporterCombo.getItemAt(exporterCombo.getSelectedIndex());
            final String extension = provider.getExtension();

            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save model as");
            chooser.setFileFilter(new FileExtensionFilter("%s Files".formatted(extension.toUpperCase()), extension));
            chooser.setSelectedFile(new File("%s.%s".formatted("exported", extension)));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(getDialog()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final EnumSet<ModelExporterProvider.Option> options = EnumSet.noneOf(ModelExporterProvider.Option.class);

            for (JCheckBox checkbox : optionCheckboxes) {
                if (checkbox.isEnabled() && checkbox.isSelected()) {
                    options.add(OPTION_KEY.get(checkbox));
                }
            }

            final Path output = chooser.getSelectedFile().toPath();

            final Boolean done = ProgressDialog.showProgressDialog(JOptionPane.getRootFrame(), "Export models", monitor -> {
                try {
                    final String name = IOUtils.getBasename(output.getFileName().toString());
                    final RTTIObject object = controller.getValue();
                    final ModelExporter exporter = provider.create(controller.getProject(), options, output);

                    try (ProgressMonitor.Task task = monitor.begin("Exporting %s".formatted(name), 2)) {
                        try (Writer writer = Files.newBufferedWriter(output)) {
                            exporter.export(task.split(1), controller.getBinary(), object, name, writer);
                        }
                    }
                    return true;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null);

            if (done == Boolean.TRUE) {
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Done");
            } else {
                IOUtils.unchecked(() -> Files.deleteIfExists(output));
            }
        }

        super.buttonPressed(descriptor);
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_SAVE, BUTTON_CANCEL};
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_SAVE;
    }
}
