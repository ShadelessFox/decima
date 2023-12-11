package com.shade.platform.ui.settings.impl;

import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;

public class SettingsTreeNodePage extends SettingsTreeNode {
    private final LazyWithMetadata<SettingsPage, SettingsPageRegistration> page;

    public SettingsTreeNodePage(@NotNull SettingsTreeNode parent, @NotNull LazyWithMetadata<SettingsPage, SettingsPageRegistration> page) {
        super(parent);
        this.page = page;
    }

    @NotNull
    @Override
    public String getLabel() {
        return page.metadata().name();
    }

    @NotNull
    @Override
    public String getId() {
        return page.metadata().id();
    }

    @Override
    public int getOrder() {
        return page.metadata().order();
    }

    @NotNull
    public SettingsPageRegistration getMetadata() {
        return page.metadata();
    }

    @NotNull
    public SettingsPage getPage() {
        return page.get();
    }
}
