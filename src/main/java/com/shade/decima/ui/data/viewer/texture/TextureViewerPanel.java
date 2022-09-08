package com.shade.decima.ui.data.viewer.texture;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.WrapLayout;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanelViewport;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class TextureViewerPanel extends JComponent implements PropertyChangeListener {
    private static final Float[] ZOOM_LEVELS = {0.1f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f, 16.0f};
    private static final float ZOOM_MIN_LEVEL = ZOOM_LEVELS[0];
    private static final float ZOOM_MAX_LEVEL = ZOOM_LEVELS[ZOOM_LEVELS.length - 1];

    protected final ImagePanelViewport imageViewport;
    protected final ImagePanel imagePanel;
    protected final JLabel statusLabel;

    private final ZoomOutAction zoomOutAction;
    private final ZoomInAction zoomInAction;
    private final ZoomFitAction zoomFitAction;
    private final ImportImageAction importImageAction;
    private final ExportImageAction exportImageAction;

    private final JComboBox<Float> zoomCombo;
    private final JComboBox<Integer> mipCombo;
    private final JComboBox<Integer> sliceCombo;

    public TextureViewerPanel() {
        imagePanel = new ImagePanel(null);
        imageViewport = new ImagePanelViewport(imagePanel);

        statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        zoomOutAction = new ZoomOutAction();
        zoomInAction = new ZoomInAction();
        zoomFitAction = new ZoomFitAction();

        importImageAction = new ImportImageAction();
        exportImageAction = new ExportImageAction();

        zoomCombo = new JComboBox<>(new ZoomComboBoxModel(imagePanel, List.of(ZOOM_LEVELS)));
        zoomCombo.setUI(new NarrowComboBoxUI());
        zoomCombo.addItemListener(e -> imagePanel.setZoom(zoomCombo.getItemAt(zoomCombo.getSelectedIndex())));
        zoomCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Float> list, @NotNull Float value, int index, boolean selected, boolean focused) {
                append("Zoom: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);

                if (imagePanel.getProvider() != null) {
                    append("%d%%".formatted((int) (value * 100)), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("?", TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        mipCombo = new JComboBox<>();
        mipCombo.setUI(new NarrowComboBoxUI());
        mipCombo.addItemListener(e -> imagePanel.setMip(mipCombo.getItemAt(mipCombo.getSelectedIndex())));
        mipCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                append("Mip: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);

                final ImageProvider provider = imagePanel.getProvider();

                if (provider != null) {
                    final int width = Math.max(provider.getMaxWidth() >> value, 1);
                    final int height = Math.max(provider.getMaxHeight() >> value, 1);
                    append("%dx%d".formatted(width, height), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("?", TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        sliceCombo = new JComboBox<>();
        sliceCombo.setUI(new NarrowComboBoxUI());
        sliceCombo.addItemListener(e -> imagePanel.setSlice(sliceCombo.getItemAt(sliceCombo.getSelectedIndex())));
        sliceCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                append("Slice: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);

                if (imagePanel.getProvider() != null) {
                    append(String.valueOf(value), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("?", TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        imagePanel.addPropertyChangeListener(this);

        final JToolBar actionToolbar = new JToolBar();
        actionToolbar.add(new ChangeColorAction());
        actionToolbar.addSeparator();
        actionToolbar.add(zoomOutAction);
        actionToolbar.add(zoomInAction);
        actionToolbar.add(zoomFitAction);
        actionToolbar.addSeparator();
        actionToolbar.add(importImageAction);
        actionToolbar.add(exportImageAction);

        final JToolBar comboToolbar = new JToolBar();
        comboToolbar.add(zoomCombo);
        comboToolbar.add(mipCombo);
        comboToolbar.add(sliceCombo);

        final JScrollPane imagePane = new JScrollPane();
        imagePane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        imagePane.setViewport(imageViewport);
        imagePane.setWheelScrollingEnabled(false);
        imagePane.addMouseWheelListener(e -> {
            if (imagePanel.getProvider() == null || e.getModifiersEx() != 0) {
                return;
            }

            final float step = 0.2f * (float) -e.getPreciseWheelRotation();
            final float oldZoom = imagePanel.getZoom();
            final float newZoom = (float) Math.exp(Math.log(oldZoom) + step);

            if ((newZoom <= oldZoom || newZoom <= 128.0f) && (newZoom >= oldZoom || newZoom >= 0.03125f)) {
                final var point = SwingUtilities.convertPoint(imageViewport, e.getX(), e.getY(), imagePanel);
                final var rect = imageViewport.getViewRect();
                rect.x = (int) (point.getX() * newZoom / oldZoom - point.getX() + rect.getX());
                rect.y = (int) (point.getY() * newZoom / oldZoom - point.getY() + rect.getY());

                if (newZoom > oldZoom) {
                    imagePanel.setZoom(newZoom);
                    imagePanel.scrollRectToVisible(rect);
                } else {
                    imagePanel.scrollRectToVisible(rect);
                    imagePanel.setZoom(newZoom);
                }
            }
        });

        final JPanel toolbars = new JPanel();
        toolbars.setLayout(new WrapLayout(WrapLayout.LEFT, 0, 0));
        toolbars.add(actionToolbar);
        toolbars.add(comboToolbar);

        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        add(imagePane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // HACK: Force visuals update
        propertyChange(new PropertyChangeEvent(this, "provider", null, null));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final ImageProvider provider = imagePanel.getProvider();
        final String name = event.getPropertyName();

        if (name.equals("provider")) {
            zoomInAction.setEnabled(provider != null);
            zoomOutAction.setEnabled(provider != null);
            zoomFitAction.setEnabled(provider != null);
            zoomCombo.setEnabled(provider != null);
            zoomCombo.setSelectedItem(imagePanel.getZoom());

            importImageAction.setEnabled(false);
            exportImageAction.setEnabled(provider != null);

            mipCombo.setEnabled(provider != null && provider.getMipCount() > 1);
            mipCombo.setModel(getRangeModel(provider != null ? provider.getMipCount() : 1));
            mipCombo.setSelectedIndex(imagePanel.getMip());
        }

        if (name.equals("provider") || name.equals("mip")) {
            sliceCombo.setEnabled(provider != null && provider.getSliceCount(imagePanel.getMip()) > 1);
            sliceCombo.setModel(getRangeModel(provider != null ? provider.getSliceCount(imagePanel.getMip()) : 1));
            sliceCombo.setSelectedIndex(imagePanel.getSlice());
        }

        if (name.equals("zoom")) {
            zoomInAction.setEnabled(imagePanel.getZoom() < ZOOM_MAX_LEVEL);
            zoomOutAction.setEnabled(imagePanel.getZoom() > ZOOM_MIN_LEVEL);
            zoomCombo.setSelectedItem(imagePanel.getZoom());
        }

        if (name.equals("mip")) {
            mipCombo.setSelectedIndex(imagePanel.getMip());
        }

        if (name.equals("slice")) {
            sliceCombo.setSelectedIndex(imagePanel.getSlice());
        }
    }

    public void setStatusText(@Nullable String text) {
        statusLabel.setText(text);
    }

    @NotNull
    public ImagePanel getImagePanel() {
        return imagePanel;
    }

    @NotNull
    private static ComboBoxModel<Integer> getRangeModel(int end) {
        return new DefaultComboBoxModel<>(IntStream.range(0, end).boxed().toArray(Integer[]::new));
    }

    private class ChangeColorAction extends AbstractAction {
        public ChangeColorAction() {
            super("Background Color", new ColorIcon(imageViewport::getBackground));
            putValue(SHORT_DESCRIPTION, "Change viewport background color");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Color color = JColorChooser.showDialog(TextureViewerPanel.this, "Choose background color", imageViewport.getBackground());

            if (color != null) {
                imageViewport.setBackground(color);
            }
        }
    }

    private class ZoomInAction extends AbstractAction {
        public ZoomInAction() {
            super("Zoom In", UIManager.getIcon("Editor.zoomInIcon"));
            putValue(SHORT_DESCRIPTION, "Zoom image in");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int index = Arrays.binarySearch(ZOOM_LEVELS, imagePanel.getZoom());
            if (index < 0) {
                index = -index;
            }
            imagePanel.setZoom(ZOOM_LEVELS[Math.min(ZOOM_LEVELS.length - 1, index + 1)]);
        }
    }

    private class ZoomOutAction extends AbstractAction {
        public ZoomOutAction() {
            super("Zoom Out", UIManager.getIcon("Editor.zoomOutIcon"));
            putValue(SHORT_DESCRIPTION, "Zoom image out");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int index = Arrays.binarySearch(ZOOM_LEVELS, imagePanel.getZoom());
            if (index < 0) {
                index = -index - 1;
            }
            imagePanel.setZoom(ZOOM_LEVELS[Math.max(0, index - 1)]);
        }
    }

    private class ZoomFitAction extends AbstractAction {
        public ZoomFitAction() {
            super("Fit to Viewport", UIManager.getIcon("Editor.zoomFitIcon"));
            putValue(SHORT_DESCRIPTION, "Fit image to viewport");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            imagePanel.fit();
        }
    }

    private class ImportImageAction extends AbstractAction {
        public ImportImageAction() {
            super("Import Image", UIManager.getIcon("Editor.importIcon"));
            putValue(SHORT_DESCRIPTION, "Import image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO
        }
    }

    private class ExportImageAction extends AbstractAction {
        public ExportImageAction() {
            super("Export Image", UIManager.getIcon("Editor.exportIcon"));
            putValue(SHORT_DESCRIPTION, "Export image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final ImageProvider provider = imagePanel.getProvider();
            if (provider != null) {
                new TextureExportDialog(provider).showDialog(Application.getFrame());
            }
        }
    }

    private static class NarrowComboBoxUI extends FlatComboBoxUI {
        @Override
        public Dimension getMaximumSize(JComponent c) {
            return getPreferredSize(c);
        }
    }

    private static class ZoomComboBoxModel implements ComboBoxModel<Float> {
        private final List<ListDataListener> listeners;
        private final List<Float> levels;
        private final ImagePanel panel;

        private int index;

        public ZoomComboBoxModel(@NotNull ImagePanel panel, @NotNull List<Float> levels) {
            this.listeners = new ArrayList<>();
            this.panel = panel;
            this.levels = levels;
            this.index = -1;
        }

        @Override
        public void setSelectedItem(Object item) {
            index = levels.indexOf((Float) item);
            fireListDataEvent(ListDataListener::contentsChanged);
        }

        @Override
        public Object getSelectedItem() {
            if (index < 0) {
                return panel.getZoom();
            } else {
                return levels.get(index);
            }
        }

        @Override
        public int getSize() {
            if (levels.contains(panel.getZoom())) {
                return levels.size();
            } else {
                return levels.size() + 1;
            }
        }

        @Override
        public Float getElementAt(int index) {
            if (index == levels.size()) {
                return panel.getZoom();
            } else {
                return levels.get(index);
            }
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        private void fireListDataEvent(@NotNull BiConsumer<ListDataListener, ListDataEvent> consumer) {
            if (listeners.isEmpty()) {
                return;
            }

            final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);

            for (ListDataListener listener : listeners) {
                consumer.accept(listener, event);
            }
        }
    }
}
