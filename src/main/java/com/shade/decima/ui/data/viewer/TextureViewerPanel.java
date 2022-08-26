package com.shade.decima.ui.data.viewer;

import com.shade.decima.ui.data.viewer.texture.component.ImagePanel;
import com.shade.decima.ui.data.viewer.texture.component.ImageViewport;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class TextureViewerPanel extends JComponent {
    private static final Float[] ZOOM_LEVELS = {0.1f, 0.25f, 0.5f, 1.0f, 2.0f, 4.0f, 8.0f};
    private static final float ZOOM_MIN_LEVEL = ZOOM_LEVELS[0];
    private static final float ZOOM_MAX_LEVEL = ZOOM_LEVELS[ZOOM_LEVELS.length - 1];

    protected final ImagePanel imagePanel;
    protected final JLabel statusLabel;

    public TextureViewerPanel() {
        imagePanel = new ImagePanel(null);
        statusLabel = new JLabel();

        final ZoomOutAction zoomOutAction = new ZoomOutAction();
        final ZoomInAction zoomInAction = new ZoomInAction();
        final ZoomFitAction zoomFitAction = new ZoomFitAction();
        final JComboBox<Float> zoomCombo = new JComboBox<>(ZOOM_LEVELS) {
            {
                setFocusable(false);
                setSelectedItem(1.0f);
                addItemListener(e -> imagePanel.setZoom((Float) e.getItem()));
                setRenderer(new ColoredListCellRenderer<>() {
                    @Override
                    protected void customizeCellRenderer(@NotNull JList<? extends Float> list, @NotNull Float value, int index, boolean selected, boolean focused) {
                        append("Zoom: ", TextAttributes.GRAYED_ATTRIBUTES);
                        append("%d%%".formatted((int) (value * 100)), TextAttributes.REGULAR_ATTRIBUTES);
                    }
                });
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };

        imagePanel.addPropertyChangeListener("zoom", event -> {
            final float zoom = (Float) event.getNewValue();
            zoomInAction.setEnabled(zoom < ZOOM_MAX_LEVEL);
            zoomOutAction.setEnabled(zoom > ZOOM_MIN_LEVEL);
            zoomCombo.setSelectedItem(zoom);
        });

        final JToolBar toolbar = new JToolBar();
        toolbar.add(zoomOutAction);
        toolbar.add(zoomInAction);
        toolbar.add(zoomFitAction);
        toolbar.add(zoomCombo);
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(statusLabel);

        final JScrollPane imagePane = new JScrollPane();
        imagePane.setViewport(new ImageViewport(imagePanel));

        setLayout(new MigLayout("ins panel", "[grow,fill]", "[][grow,fill]"));
        add(toolbar, "wrap");
        add(imagePane);
    }

    public void setStatusText(@Nullable String text) {
        statusLabel.setText(text);
    }

    public void setImage(@Nullable Image image) {
        imagePanel.setImage(image);
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
}
