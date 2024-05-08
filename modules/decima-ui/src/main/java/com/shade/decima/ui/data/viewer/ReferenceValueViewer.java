package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.data.ObjectValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

@ValueViewerRegistration({
    @Selector(type = @Type(type = RTTIReference.class))
})
public class ReferenceValueViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new WrapperPanel();
    }

    @Override
    public void refresh(@NotNull ProgressMonitor monitor, @NotNull JComponent component, @NotNull ValueController<?> controller) {
        final Result data = Objects.requireNonNull(getObjectWithViewer(controller));
        final WrapperPanel panel = (WrapperPanel) component;

        if (panel.viewer != data.viewer) {
            panel.viewer = data.viewer;
            panel.removeAll();
            panel.add(UIUtils.invokeAndWait(() -> {
                final JComponent comp = data.viewer.createComponent();
                component.removeAll();
                component.add(comp, BorderLayout.CENTER);
                return comp;
            }));
        }

        data.viewer.refresh(monitor, (JComponent) panel.getComponent(0), data.controller);
    }

    @Override
    public boolean canView(@NotNull ValueController<?> controller) {
        return getObjectWithViewer(controller) != null;
    }

    @Nullable
    private static Result getObjectWithViewer(@NotNull ValueController<?> controller) {
        final RTTIReference reference = (RTTIReference) controller.getValue();
        final RTTIReference.FollowResult result;

        try {
            result = reference.follow(controller.getProject(), controller.getCoreFile());
        } catch (IOException e) {
            return null;
        }

        if (result == null) {
            return null;
        }

        final ValueController<?> newController = new ObjectValueController(controller, result.file(), result.object());
        final ValueViewer newViewer = ValueRegistry.getInstance().findViewer(newController);

        if (newViewer == null) {
            return null;
        }

        return new Result(result.object(), newController, newViewer);
    }

    private record Result(@NotNull RTTIObject object, @NotNull ValueController<?> controller, @NotNull ValueViewer viewer) {}

    private static class WrapperPanel extends JComponent {
        private ValueViewer viewer;

        public WrapperPanel() {
            setLayout(new BorderLayout());
        }
    }
}
