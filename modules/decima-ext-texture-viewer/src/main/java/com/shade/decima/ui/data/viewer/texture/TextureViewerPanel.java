package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.ui.controls.WrapLayout;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanel;
import com.shade.decima.ui.data.viewer.texture.controls.ImagePanelViewport;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.settings.TextureViewerSettings;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.RangeSlider;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.stream.IntStream;

public class TextureViewerPanel extends JComponent implements PropertyChangeListener, Disposable {
    public static final DataKey<ImagePanelViewport> VIEWPORT_KEY = new DataKey<>("viewport", ImagePanelViewport.class);
    public static final DataKey<ImagePanel> PANEL_KEY = new DataKey<>("panel", ImagePanel.class);
    public static final DataKey<ImageProvider> PROVIDER_KEY = new DataKey<>("provider", ImageProvider.class);
    public static final DataKey<Float> ZOOM_KEY = new DataKey<>("zoom", Float.class);

    public static final float ZOOM_MIN_LEVEL = (float) Math.pow(2, -5);
    public static final float ZOOM_MAX_LEVEL = (float) Math.pow(2, 7);
    public static final float RANGE_PRECISION = 1e3f;

    private static final String[] CUBEMAP_FACE_NAMES = {"X+", "X-", "Y+", "Y-", "Z+", "Z-"};

    protected final ImagePanelViewport imageViewport;
    protected final ImagePanel imagePanel;
    protected final JLabel statusLabel;

    private final MessageBusConnection connection;

    private final JToolBar actionToolbar;
    private final JToolBar statusToolbar;

    private final JComboBox<Integer> mipCombo;
    private final JComboBox<Integer> sliceCombo;
    private final RangeSlider rangeSlider;

    public TextureViewerPanel() {
        connection = MessageBus.getInstance().connect();
        connection.subscribe(TextureViewerSettings.SETTINGS, this::repaint);

        imagePanel = new ImagePanel(null);
        imageViewport = new ImagePanelViewport(imagePanel);

        statusLabel = new JLabel();
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        mipCombo = new JComboBox<>();
        mipCombo.addItemListener(e -> imagePanel.setMip(mipCombo.getItemAt(mipCombo.getSelectedIndex())));
        mipCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                append("Mip: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);

                final ImageProvider provider = imagePanel.getProvider();

                if (provider != null) {
                    final int width = Math.max(provider.getMaxWidth() >> value, 1);
                    final int height = Math.max(provider.getMaxHeight() >> value, 1);
                    append("%d - %dx%d".formatted(value, width, height), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("?", TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        sliceCombo = new JComboBox<>();
        sliceCombo.addItemListener(e -> imagePanel.setSlice(sliceCombo.getItemAt(sliceCombo.getSelectedIndex())));
        sliceCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Integer> list, @NotNull Integer value, int index, boolean selected, boolean focused) {
                if (imagePanel.getProvider() == null) {
                    append("Slice: ?", TextAttributes.REGULAR_ATTRIBUTES);
                } else if (imagePanel.getProvider().getType() == ImageProvider.Type.CUBEMAP) {
                    append("Face: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                    append(CUBEMAP_FACE_NAMES[value], TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("Slice: ", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                    append(String.valueOf(value), TextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        });

        rangeSlider = new RangeSlider(0, 1, 0, 1);
        rangeSlider.setToolTipText("Visible range points");
        rangeSlider.addChangeListener(e -> imagePanel.setRange(
            rangeSlider.getLowValue() / RANGE_PRECISION,
            rangeSlider.getHighValue() / RANGE_PRECISION
        ));

        imagePanel.addPropertyChangeListener(this);
        imageViewport.addPropertyChangeListener(this);

        final JScrollPane imagePane = new JScrollPane();
        imagePane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.shadow")));
        imagePane.setViewport(imageViewport);
        imagePane.setWheelScrollingEnabled(false);
        imagePane.addMouseWheelListener(e -> {
            if (imagePanel.getProvider() == null || e.getModifiersEx() != 0) {
                return;
            }

            final float step = 0.2f * (float) -e.getPreciseWheelRotation();
            final float oldZoom = imagePanel.getZoom();
            final float newZoom = IOUtils.clamp((float) Math.exp(Math.log(oldZoom) + step), ZOOM_MIN_LEVEL, ZOOM_MAX_LEVEL);

            if (oldZoom != newZoom) {
                final var point = SwingUtilities.convertPoint(imageViewport, e.getX(), e.getY(), imagePanel);
                final var rect = imageViewport.getViewRect();
                rect.x = (int) Math.round(point.getX() * newZoom / oldZoom - point.getX() + rect.getX());
                rect.y = (int) Math.round(point.getY() * newZoom / oldZoom - point.getY() + rect.getY());

                if (newZoom > oldZoom) {
                    imagePanel.setZoom(newZoom);
                    imagePanel.scrollRectToVisible(rect);
                } else {
                    imagePanel.scrollRectToVisible(rect);
                    imagePanel.setZoom(newZoom);
                }
            }
        });

        final DataContext context = key -> switch (key) {
            case "viewport" -> imageViewport;
            case "panel" -> imagePanel;
            case "provider" -> imagePanel.getProvider();
            case "zoom" -> imagePanel.getZoom();
            default -> null;
        };

        actionToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_TEXTURE_VIEWER_ID, context);
        statusToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_TEXTURE_VIEWER_BOTTOM_ID, context);
        statusToolbar.addSeparator();
        statusToolbar.add(rangeSlider);
        statusToolbar.addSeparator();
        statusToolbar.add(statusLabel);

        final JToolBar comboToolbar = new JToolBar();
        comboToolbar.add(Box.createHorizontalStrut(4));
        comboToolbar.add(mipCombo);
        comboToolbar.add(Box.createHorizontalStrut(4));
        comboToolbar.add(sliceCombo);

        final JPanel toolbars = new JPanel();
        toolbars.setLayout(new WrapLayout(WrapLayout.LEFT, 0, 0));
        toolbars.add(actionToolbar);
        toolbars.add(comboToolbar);

        setLayout(new BorderLayout());
        add(toolbars, BorderLayout.NORTH);
        add(imagePane, BorderLayout.CENTER);
        add(statusToolbar, BorderLayout.SOUTH);

        // HACK: Force visuals update
        propertyChange(new PropertyChangeEvent(this, "provider", null, null));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final ImageProvider provider = imagePanel.getProvider();
        final String name = event.getPropertyName();

        if (name.equals("provider")) {
            mipCombo.setEnabled(provider != null && provider.getMipCount() > 1);
            mipCombo.setModel(getRangeModel(provider != null ? provider.getMipCount() : 1));
            mipCombo.setSelectedIndex(imagePanel.getMip());
        }

        if (name.equals("provider") || name.equals("mip")) {
            sliceCombo.setEnabled(provider != null && provider.getSliceCount(imagePanel.getMip()) > 1);
            sliceCombo.setModel(getRangeModel(provider != null ? provider.getSliceCount(imagePanel.getMip()) : 1));
            sliceCombo.setSelectedIndex(imagePanel.getSlice());
        }

        if (name.equals("provider") || name.equals("mip") || name.equals("slice")) {
            if (provider != null && imagePanel.isRangeAdjustable()) {
                final float[] range = imagePanel.computeRange();
                final int min = (int) (range[0] * RANGE_PRECISION);
                final int max = (int) (range[1] * RANGE_PRECISION);

                rangeSlider.setEnabled(true);
                rangeSlider.setMinimum(min);
                rangeSlider.setLowValue(min);
                rangeSlider.setMaximum(max);
                rangeSlider.setHighValue(max);
            } else {
                rangeSlider.setEnabled(false);
            }
        }

        if (name.equals("mip")) {
            mipCombo.setSelectedIndex(imagePanel.getMip());
        }

        if (name.equals("slice")) {
            sliceCombo.setSelectedIndex(imagePanel.getSlice());
        }

        if (name.equals("provider") || name.equals("zoom") || name.equals("channels") || name.equals("background")) {
            final MenuManager manager = MenuManager.getInstance();
            manager.update(actionToolbar);
            manager.update(statusToolbar);
        }
    }

    @Override
    public void dispose() {
        connection.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 0);
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
}
