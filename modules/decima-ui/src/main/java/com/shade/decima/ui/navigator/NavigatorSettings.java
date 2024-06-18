package com.shade.decima.ui.navigator;

import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;


@Service(NavigatorSettings.class)
@Persistent("NavigatorSettings")
public class NavigatorSettings implements PersistableComponent<NavigatorSettings> {
    public enum PackfileView {
        DEFAULT("Default"),
        GROUPED("Grouped"),
        MERGED("Merged");

        private final String label;

        PackfileView(@NotNull String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DirectoryView {
        DEFAULT("Default"),
        FLATTEN("Flatten"),
        COMPACT("Compact");

        private final String label;

        DirectoryView(@NotNull String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public PackfileView packfileView = PackfileView.DEFAULT;
    public DirectoryView directoryView = DirectoryView.COMPACT;

    @NotNull
    public static NavigatorSettings getInstance() {
        return ApplicationManager.getApplication().getService(NavigatorSettings.class);
    }

    @Nullable
    @Override
    public NavigatorSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull NavigatorSettings state) {
        packfileView = state.packfileView;
        directoryView = state.directoryView;
    }
}
