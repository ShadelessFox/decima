package com.shade.decima.ui.controls;

import com.shade.platform.model.util.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class MemoryIndicator extends JProgressBar {
    public MemoryIndicator() {
        final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

        final Timer timer = new Timer(1000, e -> {
            final MemoryUsage usage = bean.getHeapMemoryUsage();
            setMaximum((int) (usage.getMax() / 1024));
            setValue((int) (usage.getUsed() / 1024));
            setString("%s of %s".formatted(IOUtils.formatSize(usage.getUsed()), IOUtils.formatSize(usage.getMax())));
            setStringPainted(true);
            setToolTipText("Max Heap Size: %s\nCommitted: %s\nUsed: %s".formatted(
                IOUtils.formatSize(usage.getMax()),
                IOUtils.formatSize(usage.getCommitted()),
                IOUtils.formatSize(usage.getUsed())
            ));
        });
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
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 22);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
