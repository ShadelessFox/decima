package com.shade.decima.ui;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.decima.BuildConfig;
import com.shade.platform.ui.UIColor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Objects;
import java.util.Random;

public class Splash {
    private static final Splash INSTANCE = new Splash();
    private SplashFrame frame;

    private Splash() {
        // Prevents instantiation
    }

    @NotNull
    public static Splash getInstance() {
        return INSTANCE;
    }

    public void show() {
        if (frame == null) {
            frame = new SplashFrame();
            frame.setVisible(true);
        }
    }

    public void hide() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public void set(@Nullable String status) {
        if (frame != null) {
            frame.component.setStatus(status);
        }
    }

    private static class SplashFrame extends JFrame {
        private final SplashComponent component;

        public SplashFrame() {
            add(component = new SplashComponent());
            setUndecorated(true);
            setSize(new Dimension(480, 260));
            setBackground(UIColor.TRANSPARENT);
            setLocationRelativeTo(null);
            setAlwaysOnTop(true);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }
    }

    private static class SplashComponent extends JComponent {
        private static final Color COLOR_1 = new Color(0xFF42C9);
        private static final Color COLOR_2 = new Color(0x8743FF);
        private static final Color COLOR_3 = new Color(0x45CDFF);

        private BufferedImage splash;
        private String status;

        public SplashComponent() {
            setFont(createFont());
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (splash == null) {
                splash = createSplashTexture(getFont(), getWidth(), getHeight());
            }

            g.drawImage(splash, 0, 0, null);

            if (status != null) {
                final Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                g2.drawString(status, 30, getHeight() - g2.getFontMetrics().getDescent() - 30);
                g2.dispose();
            }
        }

        public void setStatus(@Nullable String status) {
            if (Objects.equals(status, this.status)) {
                return;
            }
            this.status = status;
            repaint();
        }

        @NotNull
        private static BufferedImage createSplashTexture(@NotNull Font font, int width, int height) {
            final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Rounded mask
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, 10, 10));
            g2.setComposite(AlphaComposite.SrcAtop);

            // Fancy rectangles
            paintRect(g2, 285, 350, 880, 880, 155);
            paintRect(g2, 200, 410, 460, 460, 10);
            paintRect(g2, 450, 410, 550, 550, 30);

            // Noise overlay
            g2.setComposite(AlphaComposite.SrcAtop.derive(0.05f));
            g2.drawImage(createNoiseTexture(width, height), 0, 0, null);

            // Text
            final Font font1 = font.deriveFont(36f);
            final Font font2 = font.deriveFont(18f);
            final FontMetrics fm1 = g2.getFontMetrics(font1);
            final FontMetrics fm2 = g2.getFontMetrics(font2);

            g2.setComposite(AlphaComposite.Src);
            g2.setColor(Color.WHITE);
            g2.setFont(font1);
            g2.drawString(BuildConfig.APP_TITLE, 30, 30 + fm1.getAscent());
            g2.setFont(font2);
            g2.drawString(BuildConfig.APP_VERSION, 32, 30 + fm1.getHeight() + fm2.getAscent());

            g2.dispose();

            return image;
        }

        @NotNull
        private static BufferedImage createNoiseTexture(int width, int height) {
            final byte[] data = new byte[width * height];
            new Random(0xDEC13A).nextBytes(data);

            final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            final WritableRaster raster = image.getRaster();
            raster.setDataElements(0, 0, width, height, data);

            return image;
        }

        @NotNull
        private static Font createFont() {
            final String name;

            // We can't rely on FlatLaf because it might not be initialized yet
            if (SystemInfo.isWindows) {
                name = "Segoe UI Light";
            } else if (SystemInfo.isMacOS) {
                name = "HelveticaNeue-Thin";
            } else if (SystemInfo.isLinux) {
                name = "SansSerif";
            } else {
                name = null;
            }

            return new Font(name, Font.PLAIN, 12);
        }

        private static void paintRect(@NotNull Graphics2D g, int x, int y, int width, int height, int degrees) {
            g.setPaint(new LinearGradientPaint(
                x - width / 2f, y,
                x + height / 2f, y,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{COLOR_1, COLOR_2, COLOR_3}
            ));
            g.rotate(Math.toRadians(degrees), x, y);
            g.fillRoundRect(x - width / 2, y - height / 2, width, height, width / 2, height / 2);
            g.rotate(-Math.toRadians(degrees), x, y);
            g.setPaint(null);
        }
    }
}
