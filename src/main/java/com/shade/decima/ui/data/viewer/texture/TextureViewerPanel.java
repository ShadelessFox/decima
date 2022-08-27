package com.shade.decima.ui.data.viewer.texture;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.controls.ImageViewport;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.stream.IntStream;

public class TextureViewerPanel extends JComponent {
    private static final Float[] ZOOM_LEVELS = {0.1f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f};
    private static final float ZOOM_MIN_LEVEL = ZOOM_LEVELS[0];
    private static final float ZOOM_MAX_LEVEL = ZOOM_LEVELS[ZOOM_LEVELS.length - 1];

    protected final ImagePanel imagePanel;
    protected final JLabel statusLabel;

    public TextureViewerPanel() {
        imagePanel = new ImagePanel(null);
        statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        final ZoomOutAction zoomOutAction = new ZoomOutAction();
        final ZoomInAction zoomInAction = new ZoomInAction();
        final ZoomFitAction zoomFitAction = new ZoomFitAction();

        final JComboBox<Float> zoomCombo = new JComboBox<>(ZOOM_LEVELS);
        zoomCombo.setUI(new NarrowComboBoxUI());
        zoomCombo.addItemListener(e -> imagePanel.setZoom((Float) e.getItem()));
        zoomCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Float> list, @NotNull Float value, int index, boolean selected, boolean focused) {
                append("Zoom: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                append("%d%%".formatted((int) (value * 100)), TextAttributes.REGULAR_ATTRIBUTES);
            }
        });

        final JComboBox<Integer> mipCombo = new JComboBox<>();
        mipCombo.setUI(new NarrowComboBoxUI());
        mipCombo.addItemListener(e -> imagePanel.setMip((Integer) e.getItem()));
        mipCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                final ImageProvider provider = imagePanel.getProvider();

                if (provider != null) {
                    final int width = Math.max(provider.getWidth() >> value, 1);
                    final int height = Math.max(provider.getHeight() >> value, 1);

                    append("Mip: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                    append("%dx%d".formatted(width, height), TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        final JComboBox<Integer> sliceCombo = new JComboBox<>();
        sliceCombo.setUI(new NarrowComboBoxUI());
        sliceCombo.addItemListener(e -> imagePanel.setSlice((Integer) e.getItem()));
        sliceCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                append("Slice: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                append(String.valueOf(value), TextAttributes.REGULAR_ATTRIBUTES);
            }
        });

        imagePanel.addPropertyChangeListener(event -> {
            switch (event.getPropertyName()) {
                case "zoom" -> {
                    final float zoom = (Float) event.getNewValue();
                    zoomInAction.setEnabled(zoom < ZOOM_MAX_LEVEL);
                    zoomOutAction.setEnabled(zoom > ZOOM_MIN_LEVEL);
                    zoomCombo.setSelectedItem(zoom);
                }
                case "provider" -> {
                    final ImageProvider provider = (ImageProvider) event.getNewValue();

                    mipCombo.setEnabled(provider != null);
                    sliceCombo.setEnabled(provider != null);

                    if (provider != null) {
                        mipCombo.setModel(new DefaultComboBoxModel<>(IntStream.range(0, provider.getMipCount()).boxed().toArray(Integer[]::new)));
                        sliceCombo.setModel(new DefaultComboBoxModel<>(IntStream.range(0, provider.getSliceCount()).boxed().toArray(Integer[]::new)));
                    }
                }
            }
        });

        final JToolBar toolbar = new JToolBar();
        toolbar.add(zoomOutAction);
        toolbar.add(zoomInAction);
        toolbar.add(zoomFitAction);
        toolbar.add(zoomCombo);
        toolbar.add(mipCombo);
        toolbar.add(sliceCombo);

        final JScrollPane imagePane = new JScrollPane();
        imagePane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        imagePane.setViewport(new ImageViewport(imagePanel));

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(imagePane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void setStatusText(@Nullable String text) {
        statusLabel.setText(text);
    }

    @NotNull
    public ImagePanel getImagePanel() {
        return imagePanel;
    }

    private class ZoomInAction extends AbstractAction {
        public ZoomInAction() {
            super("Zoom In", UIManager.getIcon("Editor.zoomInIcon"));
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
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            imagePanel.fit();
        }
    }

    private static class NarrowComboBoxUI extends FlatComboBoxUI {
        @Override
        public Dimension getMaximumSize(JComponent c) {
            return getPreferredSize(c);
        }
    }
}
