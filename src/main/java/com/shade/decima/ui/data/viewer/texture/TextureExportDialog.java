package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;

import static java.nio.file.StandardOpenOption.*;

public class TextureExportDialog extends BaseDialog {
    private static final DataKey<TextureExporter.Option> OPTION_KEY = new DataKey<>("option", TextureExporter.Option.class);

    private final ImageProvider provider;

    private final JComboBox<TextureExporter> exporterCombo;
    private final List<JCheckBox> optionCheckboxes;

    public TextureExportDialog(@NotNull ImageProvider provider) {
        super("Export Texture", List.of(BUTTON_SAVE, BUTTON_CANCEL));
        this.provider = provider;

        this.optionCheckboxes = new ArrayList<>();

        for (TextureExporter.Option option : TextureExporter.Option.values()) {
            final JCheckBox checkbox = new JCheckBox(option.getLabel(), option.isEnabledByDefault());
            checkbox.putClientProperty(OPTION_KEY, option);
            optionCheckboxes.add(checkbox);
        }

        final TextureExporter[] exporters = ServiceLoader.load(TextureExporter.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(TextureExporter[]::new);

        this.exporterCombo = new JComboBox<>(exporters);
        this.exporterCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends TextureExporter> list, @NotNull TextureExporter value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
        this.exporterCombo.addItemListener(e -> {
            final TextureExporter exporter = (TextureExporter) e.getItem();
            for (JCheckBox checkbox : optionCheckboxes) {
                checkbox.setEnabled(exporter.supports(OPTION_KEY.get(checkbox)));
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
        options.setBorder(new LabeledBorder(new JLabel("Options")));

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
            final TextureExporter exporter = exporterCombo.getItemAt(exporterCombo.getSelectedIndex());
            final String extension = exporter.getExtension();

            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save texture as");
            chooser.setFileFilter(new FileExtensionFilter("%s Files".formatted(extension.toUpperCase()), extension));
            chooser.setSelectedFile(new File("exported.%s".formatted(extension)));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(getDialog()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final EnumSet<TextureExporter.Option> options = EnumSet.noneOf(TextureExporter.Option.class);

            for (JCheckBox checkbox : optionCheckboxes) {
                if (checkbox.isEnabled() && checkbox.isSelected()) {
                    options.add(OPTION_KEY.get(checkbox));
                }
            }

            try (SeekableByteChannel channel = Files.newByteChannel(chooser.getSelectedFile().toPath(), WRITE, CREATE, TRUNCATE_EXISTING)) {
                exporter.export(provider, options, channel);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        super.buttonPressed(descriptor);
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_SAVE;
    }
}
