package com.shade.platform.ui.settings;

import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.util.NotNull;

import java.util.List;

public class SettingsRegistry {
    private static final SettingsRegistry INSTANCE = new SettingsRegistry();

    private final List<LazyWithMetadata<SettingsPage, SettingsPageRegistration>> pages;

    private SettingsRegistry() {
        this.pages = ExtensionRegistry.getExtensions(SettingsPage.class, SettingsPageRegistration.class);
    }

    @NotNull
    public static SettingsRegistry getInstance() {
        return INSTANCE;
    }

    @NotNull
    public LazyWithMetadata<SettingsPage, SettingsPageRegistration> getPageById(@NotNull String id) {
        for (LazyWithMetadata<SettingsPage, SettingsPageRegistration> page : pages) {
            if (page.metadata().id().equals(id)) {
                return page;
            }
        }

        throw new IllegalArgumentException("Can't find page with id '" + id + "'");
    }

    @NotNull
    public List<LazyWithMetadata<SettingsPage, SettingsPageRegistration>> getPages(@NotNull String parent) {
        return pages.stream()
            .filter(page -> page.metadata().parent().equals(parent))
            .toList();
    }

    public boolean hasPages(@NotNull String parent) {
        for (LazyWithMetadata<SettingsPage, SettingsPageRegistration> page : pages) {
            if (page.metadata().parent().equals(parent)) {
                return true;
            }
        }

        return false;
    }
}
