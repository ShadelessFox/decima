package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;

@ValueViewerRegistration({
    @Selector(type = @Type(name = "UITexture")),
    @Selector(type = @Type(name = "ImageMapEntry")),
    @Selector(type = @Type(name = "ButtonIcon")),
    @Selector(type = @Type(name = "MenuStreamingTexture"))
})
public class UITextureViewer extends TextureViewer {
    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        RTTIObject value = (RTTIObject) controller.getValue();
        final PackfileManager manager = controller.getProject().getPackfileManager();
        final Project project = controller.getProject();
        CoreBinary binary = ((CoreEditor) controller.getEditor()).getBinary();

        try {
            if (!value.type().getTypeName().equals("UITexture")) {
                value = value.ref("Texture").get(project, binary);
            }
            value = (value.obj("BigTexture") != null) ? value.obj("BigTexture") : value.obj("SmallTexture");

            final HwTextureHeader header = value.obj("Header").cast();
            final RTTIObject texture = value;
            final TextureViewerPanel panel = (TextureViewerPanel) component;

            panel.setStatusText("%sx%s (%s, %s)".formatted(
                header.getWidth(), header.getHeight(),
                header.getType(), header.getPixelFormat()));

            SwingUtilities.invokeLater(() -> {
                panel.getImagePanel().setProvider(getImageProvider(texture, manager));
                panel.getImagePanel().fit();
                panel.revalidate();
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
