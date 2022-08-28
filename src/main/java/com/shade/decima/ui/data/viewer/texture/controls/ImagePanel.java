package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImagePanel extends JComponent implements Scrollable {
    private ImageProvider provider;
    private Image sourceImage;
    private Image scaledImage;
    private float zoom;
    private int mip;
    private int slice;

    public ImagePanel(@Nullable ImageProvider provider) {
        this.provider = provider;
        this.zoom = 1.0f;
        this.mip = 0;
        this.slice = 0;

        final Handler handler = new Handler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
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
    public ImageProvider getProvider() {
        return provider;
    }

    public void setProvider(@Nullable ImageProvider provider) {
        if (this.provider != provider) {
            final ImageProvider oldProvider = this.provider;

            this.provider = provider;
            this.sourceImage = null;
            this.scaledImage = null;
            this.zoom = 1.0f;
            this.mip = 0;
            this.slice = 0;

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

            this.mip = mip;
            this.sourceImage = null;
            this.scaledImage = null;

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

            this.slice = slice;
            this.sourceImage = null;
            this.scaledImage = null;

            update();

            firePropertyChange("slice", oldSlice, slice);
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        if (this.zoom != zoom && zoom > 0.0f) {
            final float oldScale = this.zoom;

            this.zoom = zoom;
            this.scaledImage = null;

            update();

            firePropertyChange("zoom", oldScale, zoom);
        }
    }

    public void fit() {
        if (provider == null) {
            return;
        }

        final Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, this);
        final float rs = (float) viewport.getWidth() / viewport.getHeight();
        final float ri = (float) sourceImage.getWidth(null) / sourceImage.getHeight(null);

        if (rs > ri) {
            setZoom((float) viewport.getHeight() / sourceImage.getHeight(null));
        } else {
            setZoom((float) viewport.getWidth() / sourceImage.getWidth(null));
        }
    }

    private void update() {
        if (provider == null) {
            return;
        }

        if (sourceImage == null) {
            sourceImage = provider.getImage(mip, slice);
        }

        if (scaledImage == null) {
            final int width = (int) Math.ceil(sourceImage.getWidth(null) * zoom);
            final int height = (int) Math.ceil(sourceImage.getHeight(null) * zoom);
            scaledImage = sourceImage.getScaledInstance(width, height, Image.SCALE_FAST);
        }

        revalidate();
        repaint();
    }

    private class Handler extends MouseAdapter {
        private Point origin;

        @Override
        public void mousePressed(MouseEvent e) {
            origin = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            origin = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin == null) {
                return;
            }

            final JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, ImagePanel.this);
            final Rectangle view = viewport.getViewRect();

            view.x += origin.x - e.getX();
            view.y += origin.y - e.getY();

            scrollRectToVisible(view);
        }
    }
}
