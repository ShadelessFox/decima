package com.shade.decima.app.ui.util;

import com.shade.util.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Fugue {
    private static final Fugue instance;

    private final BufferedImage sheet;
    private final Map<String, Rectangle> locations;
    private final Map<String, IconDescriptor> cache;

    static {
        try {
            instance = load(Fugue.class.getClassLoader());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Fugue(@NotNull BufferedImage sheet, @NotNull Map<String, Rectangle> locations) {
        this.sheet = sheet;
        this.locations = Map.copyOf(locations);
        this.cache = new ConcurrentHashMap<>();
    }

    @NotNull
    public static Icon getIcon(@NotNull String name) {
        return instance.getIconDescriptor(name).icon();
    }

    @NotNull
    public static Image getImage(@NotNull String name) {
        return instance.getIconDescriptor(name).image();
    }

    @NotNull
    private static Fugue load(@NotNull ClassLoader cl) throws IOException {
        try (
            InputStream iconsInputStream = cl.getResourceAsStream("com/shade/decima/app/ui/util/fugue.png");
            InputStream namesInputStream = cl.getResourceAsStream("com/shade/decima/app/ui/util/fugue.txt")
        ) {
            if (namesInputStream == null || iconsInputStream == null) {
                throw new IOException("Failed to load Fugue sheet");
            }

            var sheet = ImageIO.read(iconsInputStream);
            var names = new BufferedReader(new InputStreamReader(namesInputStream)).lines().toList();

            var size = 16;
            var stride = sheet.getWidth() / size;

            var locations = new HashMap<String, Rectangle>();
            for (int i = 0; i < names.size(); i++) {
                var name = names.get(i);
                var x = i % stride * size;
                var y = i / stride * size;
                locations.put(name, new Rectangle(x, y, size, size));
            }

            return new Fugue(sheet, locations);
        }
    }

    @NotNull
    private IconDescriptor getIconDescriptor(@NotNull String name) {
        return cache.computeIfAbsent(name, key -> {
            var location = locations.get(key);
            if (location == null) {
                throw new IllegalArgumentException("Unknown icon: " + key);
            }
            var image = sheet.getSubimage(location.x, location.y, location.width, location.height);
            var icon = new ImageIcon(image);
            return new IconDescriptor(image, icon);
        });
    }

    private record IconDescriptor(@NotNull Image image, @NotNull Icon icon) {}
}
