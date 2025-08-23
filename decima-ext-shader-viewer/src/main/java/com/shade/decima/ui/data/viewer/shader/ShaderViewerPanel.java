package com.shade.decima.ui.data.viewer.shader;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwShader;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class ShaderViewerPanel extends JComponent {
    private final JTabbedPane pane;
    private HwShader shader;

    public ShaderViewerPanel() {
        this.pane = new JTabbedPane();

        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    public void setInput(@NotNull ValueController<RTTIObject> controller) {
        final ByteBuffer buffer = ByteBuffer
            .wrap(controller.getValue().get("ExtraData"))
            .order(ByteOrder.LITTLE_ENDIAN);

        this.shader = HwShader.read(buffer, controller.getProject().getContainer().getType());
        updateTabs();
    }

    private void updateTabs() {
        pane.removeAll();

        for (HwShader.Entry entry : shader.programs()) {
            if (entry.program().blob().length == 0) {
                continue;
            }

            pane.addTab(entry.programType().toString(), new ProgramPanel(entry));
        }
    }

    private static class ProgramPanel extends JComponent {
        private final HwShader.Entry entry;

        public ProgramPanel(@NotNull HwShader.Entry entry) {
            this.entry = entry;

            final JTextArea area = new JTextArea("// No decompiled data");
            area.setFont(UIUtils.getMonospacedFont());
            area.setEditable(false);

            final JButton button = new JButton("Decompile");
            button.setMnemonic('D');
            button.addActionListener(e -> {
                final String text = ShaderUtils.decompile(entry);
                area.setText(text);
            });

            final JToolBar mainToolbar = new JToolBar();
            mainToolbar.add(button);
            mainToolbar.add(new ExportAction());

            setLayout(new BorderLayout());
            add(UIUtils.createScrollPane(area, 0, 0, 1, 0), BorderLayout.CENTER);
            add(mainToolbar, BorderLayout.SOUTH);
        }

        private class ExportAction extends AbstractAction {
            public ExportAction() {
                putValue(SMALL_ICON, UIManager.getIcon("Action.exportIcon"));
                putValue(SHORT_DESCRIPTION, "Export binary data");
                setEnabled(true);
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                final JFileChooser chooser = new FileChooser();
                chooser.setDialogTitle("Export binary data as");
                chooser.setSelectedFile(new File("exported.bin"));
                chooser.setAcceptAllFileFilterUsed(true);

                if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                try {
                    Files.write(chooser.getSelectedFile().toPath(), entry.program().blob());
                } catch (IOException e) {
                    UIUtils.showErrorDialog(e, "Error exporting data");
                }
            }
        }
    }
}
