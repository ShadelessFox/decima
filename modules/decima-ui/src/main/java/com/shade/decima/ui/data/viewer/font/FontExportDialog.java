package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.util.Objects;
import java.util.ServiceLoader;

import static java.nio.file.StandardOpenOption.*;

public class FontExportDialog extends BaseDialog {
    private final HwFont font;

    private final JComboBox<FontExporter> exporterCombo;

    public FontExportDialog(@NotNull HwFont font) {
        super("Export Font");
        this.font = font;

        final FontExporter[] exporters = ServiceLoader.load(FontExporter.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(FontExporter[]::new);

        this.exporterCombo = new JComboBox<>(exporters);
        this.exporterCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends FontExporter> list, @NotNull FontExporter value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0", "[grow,fill]", "[][][grow,fill]"));
        panel.add(new JLabel("Output format:"), "wrap");
        panel.add(exporterCombo, "wrap");

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_SAVE) {
            final FontExporter exporter = exporterCombo.getItemAt(exporterCombo.getSelectedIndex());
            final String name = Objects.requireNonNullElse(font.getName(), "font");
            final String extension = exporter.getExtension();

            final JFileChooser chooser = new FileChooser();
            chooser.setDialogTitle("Save font as");
            chooser.setFileFilter(new FileExtensionFilter("%s Files".formatted(extension.toUpperCase()), extension));
            chooser.setSelectedFile(new File("%s.%s".formatted(name, extension)));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(getDialog()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try (SeekableByteChannel channel = Files.newByteChannel(chooser.getSelectedFile().toPath(), WRITE, CREATE, TRUNCATE_EXISTING)) {
                exporter.export(font, channel);
            } catch (Exception e) {
                throw new RuntimeException(e);
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
