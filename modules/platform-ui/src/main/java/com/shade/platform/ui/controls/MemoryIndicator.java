package com.shade.platform.ui.controls;

import com.shade.platform.ui.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class MemoryIndicator extends JComponent {
    private static final int MB = 1024 * 1024;
    private static final int UPDATE_INTERVAL = 1000;

    private final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
    private MemoryUsage usage = bean.getHeapMemoryUsage();

    public MemoryIndicator() {
        final Timer timer = new Timer(UPDATE_INTERVAL, e -> refresh());
        timer.setInitialDelay(0);

        addHierarchyListener(e -> {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                bean.gc();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                repaint();
            }
        });

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void refresh() {
        usage = bean.getHeapMemoryUsage();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();

        final int width = getWidth();
        final int height = getHeight();
        final int used = (int) (width * usage.getUsed() / usage.getMax());
        final int committed = (int) (width * usage.getCommitted() / usage.getMax());

        g2.setColor(UIManager.getColor("MemoryIndicator.usedBackground"));
        g2.fillRect(0, 0, used, height);
        g2.setColor(UIManager.getColor("MemoryIndicator.committedBackground"));
        g2.fillRect(used, 0, committed - used, height);
        g2.setColor(UIManager.getColor("MemoryIndicator.maxBackground"));
        g2.fillRect(committed, 0, width - committed, height);

        UIUtils.setRenderingHints(g2);
        g2.setColor(UIManager.getColor(getMousePosition() != null ? "MemoryIndicator.hoverForeground" : "MemoryIndicator.foreground"));
        g2.setFont(UIManager.getFont("MemoryIndicator.font"));

        final FontMetrics metrics = g2.getFontMetrics();
        final String text = "%d of %dM".formatted(usage.getUsed() / MB, usage.getMax() / MB);
        g2.drawString(text, (width - metrics.stringWidth(text)) / 2, (height - metrics.getHeight()) / 2 + metrics.getAscent());

        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return """
            <html>
            <table>
            <tr><td>Used:</td><td align=right>%sM</td></tr>
            <tr><td>Committed:</td><td align=right>%sM</td></tr>
            <tr><td>Max:</td><td align=right>%sM</td></tr>
            </table>
            </html>"""
            .formatted(
                usage.getUsed() / MB,
                usage.getCommitted() / MB,
                usage.getMax() / MB
            );
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return new Dimension(100, 22);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return getPreferredSize();
    }
}
