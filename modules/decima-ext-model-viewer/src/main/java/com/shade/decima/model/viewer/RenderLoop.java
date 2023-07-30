package com.shade.decima.model.viewer;

import com.shade.util.NotNull;
import org.lwjgl.opengl.awt.AWTGLCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RenderLoop extends Thread {
    private final Window window;
    private final AWTGLCanvas canvas;

    private final Handler handler;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean isThrottling = new AtomicBoolean(false);

    private final Lock renderLock = new ReentrantLock();
    private final Condition canRender = renderLock.newCondition();

    public RenderLoop(@NotNull Window window, @NotNull AWTGLCanvas canvas) {
        super("Render Loop");

        this.window = window;
        this.canvas = canvas;
        this.handler = new Handler();

        canvas.addHierarchyListener(handler);
        window.addWindowListener(handler);
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                renderLock.lock();

                while (isThrottling.get()) {
                    canRender.awaitUninterruptibly();
                }
            } finally {
                renderLock.unlock();
            }

            try {
                SwingUtilities.invokeAndWait(() -> {
                    beforeRender();

                    if (canvas.isValid()) {
                        canvas.render();
                    }

                    afterRender();
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }

        canvas.removeHierarchyListener(handler);
        window.removeWindowListener(handler);
    }

    public void dispose() {
        isRunning.set(false);
    }

    protected void beforeRender() {
        // do nothing by default
    }

    protected void afterRender() {
        // do nothing by default
    }

    private class Handler extends WindowAdapter implements HierarchyListener {
        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                handle();
            }
        }

        @Override
        public void windowActivated(WindowEvent e) {
            handle();
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            handle();
        }

        private void handle() {
            renderLock.lock();

            try {
                isThrottling.set(!canvas.isShowing() || !window.isActive());
                canRender.signal();
            } finally {
                renderLock.unlock();
            }
        }
    }
}
