package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BinaryViewerPanel extends JPanel {
    private final HexEditor editor;
    private ValueController<byte[]> controller;

    public BinaryViewerPanel() {
        this.editor = new HexEditor();

        final JToolBar toolbar = new JToolBar();
        toolbar.add(new ExportAction());
        toolbar.add(new ImportAction());

        final JScrollPane pane = new JScrollPane(editor);
        pane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
    }

    public void setController(@NotNull ValueController<byte[]> controller) {
        this.controller = controller;
        this.editor.setModel(new DefaultHexModel(controller.getValue()));
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.exportIcon"));
            putValue(SHORT_DESCRIPTION, "Export binary data");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export binary data as");
            chooser.setSelectedFile(new File("exported.bin"));
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showSaveDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try {
                Files.write(chooser.getSelectedFile().toPath(), ((DefaultHexModel) editor.getModel()).data());
            } catch (IOException e) {
                UIUtils.showErrorDialog(Application.getFrame(), e, "Error exporting data");
            }
        }
    }

    private class ImportAction extends AbstractAction {
        public ImportAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.importIcon"));
            putValue(SHORT_DESCRIPTION, "Import binary data");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Import binary data");
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showOpenDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try {
                final byte[] data = Files.readAllBytes(chooser.getSelectedFile().toPath());
                controller.setValue(data);
                editor.setModel(new DefaultHexModel(data));
            } catch (IOException e) {
                UIUtils.showErrorDialog(Application.getFrame(), e, "Error importing data");
            }
        }
    }
}
