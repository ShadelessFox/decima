package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.decima.ui.data.viewer.texture.settings.TextureViewerSettings;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.decima.ui.data.viewer.texture.util.ChannelFilter;
import com.shade.decima.ui.data.viewer.texture.util.ClipRangeProducer;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class ImagePanel extends JComponent implements Scrollable {
    private static final Logger log = LoggerFactory.getLogger(ImagePanel.class);

    private static final String PLACEHOLDER_TEXT = "Unsupported texture format";

    private ImageProvider provider;
    private BufferedImage image;
    private float zoom;
    private float highRange;
    private float lowRange;
    private int mip;
    private int slice;
    private boolean snapZoom;
    private Set<Channel> channels;

    private BufferedImage filteredImage;
    private boolean filterDirty = true;

    public ImagePanel(@Nullable ImageProvider provider) {
        reset(provider);

        Robot robot = null;

        try {
            robot = new Robot();
        } catch (AWTException e) {
            log.warn("Can't create robot", e);
        }

        final Handler handler = new Handler(robot);
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final TextureViewerSettings settings = TextureViewerSettings.getInstance();
        final Graphics2D g2 = (Graphics2D) g.create();
        final AffineTransform tx = g2.getTransform();

        if (filteredImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            if (settings.showOutline) {
                g2.setColor(Color.RED);
                g2.drawRect(0, 0, (int) (filteredImage.getWidth() * zoom - 1), (int) (filteredImage.getHeight() * zoom - 1));
            }

            g2.scale(zoom, zoom);
            g2.drawImage(filteredImage, 0, 0, null);

            if (settings.showGrid && settings.showGridWhenZoomEqualOrMoreThan <= zoom) {
                g2.setTransform(tx);
                g2.setColor(Color.LIGHT_GRAY);

                for (int x = 1; x < filteredImage.getWidth(); x += settings.showGridEveryNthPixel) {
                    g2.drawLine((int) (x * zoom), 0, (int) (x * zoom), (int) (image.getHeight() * zoom));
                }

                for (int y = 1; y < filteredImage.getHeight(); y += settings.showGridEveryNthPixel) {
                    g2.drawLine(0, (int) (y * zoom), (int) (image.getWidth() * zoom), (int) (y * zoom));
                }
            }
        } else {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(getForeground());
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            UIUtils.setRenderingHints(g2);
            UIUtils.drawCenteredString(g2, PLACEHOLDER_TEXT, getWidth(), getHeight());
        }

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(Math.round(image.getWidth() * zoom), Math.round(image.getHeight() * zoom));
        } else {
            final Font font = getFont();
            final FontMetrics metrics = getFontMetrics(font);
            final Rectangle bounds = font.getStringBounds(PLACEHOLDER_TEXT, metrics.getFontRenderContext()).getBounds();
            return new Dimension(bounds.width + 8, bounds.height + 4);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getScrollableBlockIncrement(visibleRect, orientation, direction) / 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width;
        } else {
            return visibleRect.height;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Nullable
    public ImageProvider getProvider() {
        return provider;
    }

    public void setProvider(@Nullable ImageProvider provider) {
        setProvider(provider, null);
    }

    public void setProvider(@Nullable ImageProvider provider, @Nullable EnumSet<Channel> channels) {
        if (this.provider != provider) {
            final ImageProvider oldProvider = this.provider;

            reset(provider);

            if (channels != null) {
                this.channels = EnumSet.copyOf(channels);
            } else if (provider != null) {
                this.channels = provider.getChannels();
            }

            update();

            firePropertyChange("provider", oldProvider, provider);
        }
    }

    public int getMip() {
        return mip;
    }

    public void setMip(int mip) {
        if (this.mip != mip) {
            final int oldMip = this.mip;

            this.mip = Objects.checkIndex(mip, provider.getMipCount());
            this.image = null;

            setSlice(Math.min(slice, provider.getSliceCount(mip) - 1));

            update();
            fit();

            firePropertyChange("mip", oldMip, mip);
        }
    }

    public int getSlice() {
        return slice;
    }

    public void setSlice(int slice) {
        if (this.slice != slice) {
            final int oldSlice = this.slice;

            this.slice = Objects.checkIndex(slice, provider.getSliceCount(mip));
            this.image = null;

            update();

            firePropertyChange("slice", oldSlice, slice);
        }
    }

    @NotNull
    public Set<Channel> getProvidedChannels() {
        return provider != null ? provider.getChannels() : Set.of();
    }

    @NotNull
    public Set<Channel> getUsedChannels() {
        return Collections.unmodifiableSet(channels);
    }

    public void setChannels(@NotNull EnumSet<Channel> channels) {
        if (!this.channels.equals(channels)) {
            final Set<Channel> oldChannels = this.channels;

            this.channels = channels;
            this.filterDirty = true;

            update();
            firePropertyChange("channels", oldChannels, channels);
        }
    }

    public void addChannel(@NotNull Channel channel) {
        if (channels.add(channel)) {
            this.filterDirty = true;

            update();
            firePropertyChange("channels", null, channel);
        }
    }

    public void removeChannel(@NotNull Channel channel) {
        if (channels.remove(channel)) {
            this.filterDirty = true;

            update();
            firePropertyChange("channels", channel, null);
        }
    }

    public boolean isSnapZoom() {
        return snapZoom;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        if (this.zoom != zoom && zoom > 0.0f) {
            final float oldScale = this.zoom;

            this.zoom = zoom;
            this.snapZoom = false;

            update();

            firePropertyChange("zoom", oldScale, zoom);
        }
    }

    @NotNull
    public float[] computeRange() {
        final WritableRaster raster = image.getRaster();
        final float[] scanline = new float[image.getWidth() * raster.getNumDataElements()];
        final float[] result = {Float.MAX_VALUE, Float.MIN_VALUE};

        for (int y = 0; y < image.getHeight(); y++) {
            raster.getDataElements(0, y, image.getWidth(), 1, scanline);

            for (float v : scanline) {
                result[0] = Math.min(result[0], v);
                result[1] = Math.max(result[1], v);
            }
        }

        setRange(result[0], result[1]);

        return result;
    }

    public void setRange(float lowRange, float highRange) {
        if (this.highRange != lowRange || this.lowRange != highRange) {
            this.highRange = lowRange;
            this.lowRange = highRange;
            this.filterDirty = true;

            update();
        }
    }

    public boolean isRangeAdjustable() {
        return image != null && image.getRaster().getTransferType() == DataBuffer.TYPE_FLOAT;
    }

    public void fit() {
        setZoom(computeFitZoom());
        snapZoom = true;
    }

    public float computeFitZoom() {
        final Container container = SwingUtilities.getAncestorOfClass(JViewport.class, this).getParent();
        final Dimension viewport = container.getSize();
        UIUtils.removeFrom(viewport, container.getInsets());

        return UIUtils.getScalingFactor(
            viewport.width, viewport.height,
            image.getWidth(), image.getHeight()
        );
    }

    private void reset(@Nullable ImageProvider provider) {
        this.provider = provider;
        this.image = null;
        this.zoom = 1.0f;
        this.highRange = 0.0f;
        this.lowRange = 1.0f;
        this.mip = 0;
        this.slice = 0;
        this.channels = EnumSet.allOf(Channel.class);
        this.filteredImage = null;
        this.filterDirty = true;
    }

    private void update() {
        if (provider != null && image == null) {
            image = provider.getImage(mip, slice);
            filterDirty = true;
        }

        if (filterDirty) {
            filteredImage = image;

            ImageProducer producer = null;

            if (isRangeAdjustable() && (highRange != 0.0f || lowRange != 1.0f) && Math.abs(highRange - lowRange) > 0.001) {
                producer = new ClipRangeProducer(image, highRange, lowRange);
            }

            if (channels.size() != Channel.values().length) {
                producer = Objects.requireNonNullElseGet(producer, image::getSource);
                producer = new FilteredImageSource(producer, new ChannelFilter(channels));
            }

            if (producer != null) {
                if (filteredImage == image) {
                    final ColorModel cm = ColorModel.getRGBdefault();
                    final WritableRaster raster = cm.createCompatibleWritableRaster(image.getWidth(), image.getHeight());
                    filteredImage = new BufferedImage(cm, raster, false, null);
                }

                final Graphics2D g = filteredImage.createGraphics();
                g.setComposite(AlphaComposite.Src);
                g.drawImage(createImage(producer), 0, 0, null);
                g.dispose();
            }

            filterDirty = false;
        }

        revalidate();
        repaint();
    }

    private class Handler extends MouseAdapter {
        private final Robot robot;
        private Point origin;

        private Handler(@Nullable Robot robot) {
            this.robot = robot;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                origin = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                origin = null;
                setCursor(null);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin == null) {
                return;
            }

            final JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, ImagePanel.this);
            final Rectangle view = viewport.getViewRect();

            final Point mouse = e.getLocationOnScreen();
            final Rectangle bounds = new Rectangle(viewport.getLocationOnScreen(), viewport.getSize());

            if (robot != null && !bounds.contains(mouse)) {
                if (mouse.x >= bounds.x + bounds.width) {
                    mouse.x = bounds.x + 1;
                } else if (mouse.x < bounds.x) {
                    mouse.x = bounds.x + bounds.width - 1;
                }

                if (mouse.y >= bounds.y + bounds.height) {
                    mouse.y = bounds.y + 1;
                } else if (mouse.y < bounds.y) {
                    mouse.y = bounds.y + bounds.height - 1;
                }

                robot.mouseMove(mouse.x, mouse.y);
                origin.x = mouse.x;
                origin.y = mouse.y;

                SwingUtilities.convertPointFromScreen(origin, ImagePanel.this);
            } else {
                view.x += origin.x - e.getX();
                view.y += origin.y - e.getY();
            }

            scrollRectToVisible(view);
        }
    }
}
