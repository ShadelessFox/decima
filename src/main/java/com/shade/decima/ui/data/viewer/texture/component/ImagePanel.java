package com.shade.decima.ui.data.viewer.texture.component;

import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

public class ImagePanel extends JComponent implements Scrollable {
    private Image sourceImage;
    private Image scaledImage;
    private float zoom;

    public ImagePanel(@Nullable Image image) {
        this.sourceImage = image;
        this.scaledImage = image;
        this.zoom = 1.0f;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (scaledImage != null) {
            g.drawImage(scaledImage, 0, 0, null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (scaledImage != null) {
            return new Dimension(scaledImage.getWidth(null), scaledImage.getHeight(null));
        } else {
            return new Dimension(0, 0);
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
    public Image getSourceImage() {
        return sourceImage;
    }

    public void setImage(@Nullable Image image) {
        if (sourceImage != image) {
            final Image oldImage = sourceImage;
            sourceImage = image;
            scaledImage = image;
            firePropertyChange("image", oldImage, image);
            rescale();
            fit();
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        if (this.zoom != zoom && zoom >= 0.0f) {
            final float oldScale = this.zoom;
            this.zoom = zoom;
            firePropertyChange("zoom", oldScale, zoom);
            rescale();
        }
    }

    public void fit() {
        if (sourceImage == null) {
            return;
        }

        final Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, this);
        final int viewportSize = Math.min(viewport.getWidth(), viewport.getHeight());
        final int imageSize = Math.max(sourceImage.getWidth(null), sourceImage.getHeight(null));

        if (viewportSize > 0 && imageSize > 0) {
            setZoom((float) viewportSize / imageSize);
        }
    }

    private void rescale() {
        if (sourceImage != null) {
            if (zoom == 1.0f) {
                scaledImage = sourceImage;
            } else {
                final int width = (int) Math.ceil(sourceImage.getWidth(null) * zoom);
                final int height = (int) Math.ceil(sourceImage.getHeight(null) * zoom);
                scaledImage = sourceImage.getScaledInstance(width, height, Image.SCALE_FAST);
            }
        }

        revalidate();
        repaint();
    }
}
