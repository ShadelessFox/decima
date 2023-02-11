package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
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

    public BinaryViewerPanel() {
        this.editor = new HexEditor();

        final JToolBar toolbar = new JToolBar();
        toolbar.add(new ExportAction());

        final JScrollPane pane = new JScrollPane(editor);
        pane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
    }

    public void setInput(@NotNull byte[] data) {
        editor.setModel(new DefaultHexModel(data));
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.exportIcon"));
            putValue(SHORT_DESCRIPTION, "Export binary data");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save binary data as");
            chooser.setSelectedFile(new File("exported.bin"));
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showSaveDialog(Application.getFrame()) == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.write(chooser.getSelectedFile().toPath(), ((DefaultHexModel) editor.getModel()).data());
                } catch (IOException e) {
                    UIUtils.showErrorDialog(Application.getFrame(), e, "Error exporting data");
                }
            }
        }
    }
}
