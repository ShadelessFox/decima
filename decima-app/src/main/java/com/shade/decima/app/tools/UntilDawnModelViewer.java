package com.shade.decima.app.tools;

import com.shade.decima.app.viewport.Camera;
import com.shade.decima.app.viewport.Viewport;
import com.shade.decima.app.viewport.renderpass.RenderMeshesPass;
import com.shade.decima.game.Converter;
import com.shade.decima.game.until_dawn.game.UntilDawnGame;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.EPlatform;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.MeshResourceBase;
import com.shade.decima.scene.Node;
import com.shade.decima.scene.Scene;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class UntilDawnModelViewer {
    public static void main(String[] args) throws IOException {
        var game = new UntilDawnGame(Path.of("D:/PlayStation Games/Until Dawn"), EPlatform.PINK);

        Viewport viewport = new Viewport();
        viewport.addRenderPass(new RenderMeshesPass());
        viewport.setCamera(new Camera(30.f, 0.01f, 1000.f));

        JComboBox<MeshResourceBase> resources = new JComboBox<>();
        resources.setModel(new DefaultComboBoxModel<>(loadResources(game, "levels.game.thegame.leveldescription_lump_850fc784-2b0b-3fc0-9177-514851bf2ab9")));
        resources.setSelectedItem(null);
        resources.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                var resource = (MeshResourceBase) value;
                var name = resource != null ? "%s (%s)".formatted(resource.meshName(), resource.getType()) : null;
                return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
            }
        });
        resources.addItemListener(e -> {
            var resource = resources.getItemAt(resources.getSelectedIndex());
            var scene = Converter.convert(resource, game, Node.class)
                .map(Scene::of)
                .orElse(null);
            viewport.setScene(scene);
        });

        JFrame frame = new JFrame("Model Viewer");
        frame.add(resources, BorderLayout.NORTH);
        frame.add(viewport, BorderLayout.CENTER);
        frame.setSize(950, 850);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static MeshResourceBase[] loadResources(UntilDawnGame game, String lump) throws IOException {
        return game.loadObjectSystem(game.pathForLumpLocation(lump)).stream()
            .filter(MeshResourceBase.class::isInstance)
            .map(MeshResourceBase.class::cast)
            .toArray(MeshResourceBase[]::new);
    }
}
