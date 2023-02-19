package com.shade.platform.ui.controls;

import com.formdev.flatlaf.ui.FlatLabelUI;
import com.formdev.flatlaf.ui.FlatTabbedPaneUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.util.Objects;

public class ToolTabbedPane extends JTabbedPane {
    private static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    private static final int TAB_HEADER_SIZE = 24;

    public ToolTabbedPane(int tabPlacement) {
        super(tabPlacement);

        setUI(new FlatVerticalTabbedPaneUI());
        addHierarchyListener(e -> {
            if (e.getChangedParent() instanceof JSplitPane splitPane) {
                getModel().addChangeListener(ev -> {
                    final int index = getSelectedIndex();
                    final Object lastDividerLocation = splitPane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY);

                    if (index < 0 && lastDividerLocation == null) {
                        splitPane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, splitPane.getDividerLocation());
                        splitPane.setDividerLocation(TAB_HEADER_SIZE);
                    } else if (index >= 0 && lastDividerLocation != null) {
                        splitPane.setDividerLocation((Integer) lastDividerLocation);
                        splitPane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, null);
                    }
                });

                splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> {
                    if (getSelectedIndex() < 0) {
                        splitPane.setDividerLocation(TAB_HEADER_SIZE);
                    }
                });
            }
        });
    }

    public int getPaneSize() {
        final JSplitPane pane = (JSplitPane) getParent();
        final Object lastDividerLocation = pane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY);
        return Objects.requireNonNullElseGet((Integer) lastDividerLocation, pane::getDividerLocation);
    }

    public boolean isPaneMinimized() {
        return ((JSplitPane) getParent()).getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY) != null;
    }

    public void setPaneSize(int size) {
        final JSplitPane pane = (JSplitPane) getParent();

        if (pane.getClientProperty(LAST_DIVIDER_LOCATION_PROPERTY) != null) {
            pane.putClientProperty(LAST_DIVIDER_LOCATION_PROPERTY, size);
        } else {
            pane.setDividerLocation(size);
        }
    }

    public void minimizePane() {
        setSelectedIndex(-1);
        validate();
        repaint();
    }

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);

        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            final JLabel label = new JLabel(title);
            label.setUI(new FlatVerticalLabelUI(tabPlacement == RIGHT));
            label.setIcon(icon);
            setTabComponentAt(index, label);
        }
    }

    private static class FlatVerticalTabbedPaneUI extends FlatTabbedPaneUI {
        @Override
        protected LayoutManager createLayoutManager() {
            return new CompactTabbedPaneLayout();
        }

        @Override
        protected MouseListener createMouseListener() {
            final MouseListener delegate = super.createMouseListener();

            return new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    delegate.mouseClicked(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (tabPane.isEnabled()) {
                        final int tabIndex = tabForCoordinate(tabPane, e.getX(), e.getY());

                        if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex) && tabIndex == tabPane.getSelectedIndex()) {
                            ((ToolTabbedPane) tabPane).minimizePane();
                            return;
                        }
                    }

                    delegate.mousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    delegate.mouseReleased(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    delegate.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    delegate.mouseExited(e);
                }
            };
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                return TAB_HEADER_SIZE;
            } else {
                return super.calculateTabWidth(tabPlacement, tabIndex, metrics);
            }

        }

        @SuppressWarnings("SuspiciousNameCombination")
        @Override
        protected Insets getTabInsets(int tabPlacement, int tabIndex) {
            final Insets insets = super.getTabInsets(tabPlacement, tabIndex);

            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                return new Insets(insets.right, insets.top, insets.left, insets.bottom);
            } else {
                return insets;
            }
        }

        private class CompactTabbedPaneLayout extends FlatTabbedPaneLayout {
            @Override
            protected boolean isContentEmpty() {
                return tabPane.getSelectedIndex() < 0 || super.isContentEmpty();
            }
        }
    }

    private static class FlatVerticalLabelUI extends FlatLabelUI {
        private final boolean clockwise;

        private final Rectangle paintIconR = new Rectangle();
        private final Rectangle paintTextR = new Rectangle();
        private final Rectangle paintViewR = new Rectangle();

        private FlatVerticalLabelUI(boolean clockwise) {
            super(false);
            this.clockwise = clockwise;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            final JLabel label = (JLabel) c;
            final String text = label.getText();
            final Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

            if (icon == null && text == null) {
                return;
            }

            final Insets insets = c.getInsets();
            final FontMetrics fm = g.getFontMetrics();

            paintViewR.x = insets.left;
            paintViewR.y = insets.top;
            paintViewR.height = c.getWidth() - (insets.left + insets.right);
            paintViewR.width = c.getHeight() - (insets.top + insets.bottom);

            paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
            paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

            final String clippedText = layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);
            final Graphics2D g2 = (Graphics2D) g;
            final AffineTransform tr = g2.getTransform();

            if (icon != null) {
                icon.paintIcon(c, g, paintIconR.x, paintIconR.y + c.getHeight() - icon.getIconHeight());
            }

            if (clockwise) {
                g2.rotate(Math.PI / 2);
                g2.translate(0, -c.getWidth());
            } else {
                g2.rotate(-Math.PI / 2);
                g2.translate(-c.getHeight(), 0);
            }

            if (text != null) {
                final int textX = paintTextR.x;
                final int textY = paintTextR.y + fm.getAscent();

                if (label.isEnabled()) {
                    paintEnabledText(label, g, clippedText, textX, textY);
                } else {
                    paintDisabledText(label, g, clippedText, textX, textY);
                }
            }

            g2.setTransform(tr);
        }

        @SuppressWarnings("SuspiciousNameCombination")
        @Override
        public Dimension getPreferredSize(JComponent c) {
            final Dimension size = super.getPreferredSize(c);
            return new Dimension(size.height, size.width);
        }
    }
}
