package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.decima.ui.controls.hex.HexModel;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.util.BufferUtils;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BinaryViewerPanel extends JPanel implements Disposable {
    private static final Inspector[] INSPECTORS = {
        new NumberInspector<>("Binary", ByteBuffer::get, x -> "%8s".formatted(Integer.toBinaryString(x & 0xff)).replace(' ', '0'), Byte.BYTES),
        new NumberInspector<>("UInt8", ByteBuffer::get, x -> String.valueOf(x & 0xff), Byte.BYTES),
        new NumberInspector<>("Int8", ByteBuffer::get, String::valueOf, Byte.BYTES),
        new NumberInspector<>("UInt16", ByteBuffer::getShort, x -> String.valueOf(x & 0xffff), Short.BYTES),
        new NumberInspector<>("Int16", ByteBuffer::getShort, String::valueOf, Short.BYTES),
        new NumberInspector<>("UInt32", ByteBuffer::getInt, Integer::toUnsignedString, Integer.BYTES),
        new NumberInspector<>("Int32", ByteBuffer::getInt, String::valueOf, Integer.BYTES),
        new NumberInspector<>("UInt64", ByteBuffer::getLong, Long::toUnsignedString, Long.BYTES),
        new NumberInspector<>("Int64", ByteBuffer::getLong, String::valueOf, Long.BYTES),
        new NumberInspector<>("Half", BufferUtils::getHalfFloat, String::valueOf, Short.BYTES),
        new NumberInspector<>("Float", ByteBuffer::getFloat, String::valueOf, Float.BYTES),
        new NumberInspector<>("Double", ByteBuffer::getDouble, String::valueOf, Double.BYTES),
        new StringInspector()
    };

    private final HexEditor editor;
    private final ImportAction importAction;
    private final ExportAction exportAction;
    private ValueController<byte[]> controller;

    public BinaryViewerPanel() {
        this.editor = new HexEditor();

        final JScrollPane editorPane = UIUtils.createBorderlessScrollPane(editor);
        editorPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                editor.setRowLength(editor.getPreferredRowLength(editorPane.getViewport().getWidth()));
            }
        });

        final InspectorTableModel inspectorTableModel = new InspectorTableModel();
        final JTable inspectorTable = new JTable(inspectorTableModel);
        inspectorTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inspectorTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        inspectorTable.getColumnModel().getColumn(0).setMaxWidth(70);

        final JScrollPane inspectorPane = new JScrollPane(inspectorTable) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIColor.SHADOW));
            }
        };
        inspectorPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        final JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pane.setLeftComponent(editorPane);
        pane.setRightComponent(inspectorPane);
        pane.setOneTouchExpandable(true);

        final JComboBox<ByteOrder> orderCombo = new JComboBox<>(new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN});
        orderCombo.addItemListener(e -> {
            inspectorTableModel.order = orderCombo.getItemAt(orderCombo.getSelectedIndex());
            inspectorTableModel.fireTableDataChanged();
        });

        final JToolBar mainToolbar = new JToolBar();
        mainToolbar.add(importAction = new ImportAction());
        mainToolbar.add(exportAction = new ExportAction());

        final JToolBar orderToolbar = new JToolBar();
        orderToolbar.add(new JLabel("Byte Order: "));
        orderToolbar.add(orderCombo);

        final JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BorderLayout());
        toolbarPanel.add(mainToolbar, BorderLayout.CENTER);
        toolbarPanel.add(orderToolbar, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
        add(toolbarPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> {
            pane.setDividerLocation(pane.getHeight() - inspectorTable.getHeight() - inspectorTable.getTableHeader().getHeight() - pane.getDividerSize());
            UIUtils.minimizePanel(pane, false);
        });
    }

    public void setController(@NotNull ValueController<byte[]> controller) {
        this.controller = controller;

        editor.setModel(new DefaultHexModel(controller.getValue()));
        importAction.setEnabled(controller instanceof MutableValueController);
        exportAction.setEnabled(true);
    }

    @Override
    public void dispose() {
        controller = null;
    }

    private class ExportAction extends AbstractAction {
        public ExportAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.exportIcon"));
            putValue(SHORT_DESCRIPTION, "Export binary data");
            setEnabled(false);
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
                Files.write(chooser.getSelectedFile().toPath(), controller.getValue());
            } catch (IOException e) {
                UIUtils.showErrorDialog(e, "Error exporting data");
            }
        }
    }

    private class ImportAction extends AbstractAction {
        public ImportAction() {
            putValue(SMALL_ICON, UIManager.getIcon("Action.importIcon"));
            putValue(SHORT_DESCRIPTION, "Import binary data");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Import binary data");
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showOpenDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try {
                final byte[] data = Files.readAllBytes(chooser.getSelectedFile().toPath());
                ((MutableValueController<byte[]>) controller).setValue(data);
                editor.setModel(new DefaultHexModel(data));
            } catch (IOException e) {
                UIUtils.showErrorDialog(e, "Error importing data");
            }
        }
    }

    private class InspectorTableModel extends AbstractTableModel {
        private ByteOrder order = ByteOrder.LITTLE_ENDIAN;

        public InspectorTableModel() {
            editor.getCaret().addChangeListener(e -> fireTableDataChanged());
        }

        @Override
        public int getRowCount() {
            return INSPECTORS.length;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Name";
                case 1 -> "Value";
                default -> null;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            return switch (column) {
                case 0 -> INSPECTORS[row].getName();
                case 1 ->
                    INSPECTORS[row].inspect(editor.getModel(), order, Math.min(editor.getCaret().getDot(), editor.getCaret().getMark()));
                default -> null;
            };
        }
    }

    private interface Inspector {
        @Nullable
        String inspect(@NotNull HexModel model, @NotNull ByteOrder order, int index);

        @NotNull
        String getName();
    }

    private static class NumberInspector<T extends Number> implements Inspector {
        private final String name;
        private final BiFunction<ByteBuffer, Integer, T> getter;
        private final Function<T, String> converter;
        private final byte[] buffer;

        public NumberInspector(@NotNull String name, @NotNull BiFunction<ByteBuffer, Integer, T> getter, @NotNull Function<T, String> converter, int size) {
            this.name = name;
            this.getter = getter;
            this.converter = converter;
            this.buffer = new byte[size];
        }

        @Nullable
        @Override
        public String inspect(@NotNull HexModel model, @NotNull ByteOrder order, int index) {
            if (buffer.length + index > model.length()) {
                return null;
            } else {
                model.get(index, buffer, 0, buffer.length);
                return converter.apply(getter.apply(ByteBuffer.wrap(buffer).order(order), 0));
            }
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    private static class StringInspector implements Inspector {
        @Nullable
        @Override
        public String inspect(@NotNull HexModel model, @NotNull ByteOrder order, int index) {
            final StringBuilder buffer = new StringBuilder("\"");

            for (int i = 0; i < 255 && i + index < model.length(); i++) {
                final byte b = model.get(index + i);
                if (b == 0) {
                    break;
                }
                if (b >= ' ' && b <= '~') {
                    buffer.append((char) b);
                } else {
                    buffer.append('\\');
                    buffer.append(IOUtils.toHexDigits(b));
                }
            }

            return buffer.append("\"").toString();
        }

        @NotNull
        @Override
        public String getName() {
            return "String";
        }
    }
}
