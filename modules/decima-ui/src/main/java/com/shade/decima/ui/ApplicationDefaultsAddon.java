package com.shade.decima.ui;

import com.formdev.flatlaf.FlatDefaultsAddon;

import java.io.InputStream;

public class ApplicationDefaultsAddon extends FlatDefaultsAddon {
    @Override
    public InputStream getDefaults(Class<?> lafClass) {
        return getClass().getResourceAsStream("/themes/%s.properties".formatted(lafClass.getSimpleName()));
    }
}
