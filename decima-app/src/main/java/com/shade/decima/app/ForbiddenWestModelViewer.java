package com.shade.decima.app;

import com.shade.decima.app.viewport.Camera;
import com.shade.decima.app.viewport.Viewport;
import com.shade.decima.app.viewport.renderpass.RenderMeshesPass;
import com.shade.decima.game.Converter;
import com.shade.decima.game.hfw.game.ForbiddenWestGame;
import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.EPlatform;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.MeshResourceBase;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.StaticMeshResource;
import com.shade.decima.game.hfw.storage.StreamingObjectReader;
import com.shade.decima.scene.Node;
import com.shade.decima.scene.Scene;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.GGUUID;
import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.StreamingGroupData;

public class ForbiddenWestModelViewer {
    private static final Logger log = LoggerFactory.getLogger(ForbiddenWestModelViewer.class);

    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        var platform = EPlatform.WinGame;
        var game = new ForbiddenWestGame(source, platform);

        log.info("Initializing UI");
        Viewport viewport = new Viewport();
        viewport.addRenderPass(new RenderMeshesPass());
        viewport.setCamera(new Camera(30.f, 0.01f, 1000.f));

        JComboBox<StreamingGroupData> groups = new JComboBox<>();
        groups.setModel(new DefaultComboBoxModel<>(loadGroups(game)));
        groups.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                var group = (StreamingGroupData) value;
                if (group == null) {
                    return super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
                }
                var name = String.valueOf(group.groupID());
                return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
            }
        });

        JComboBox<MeshResourceBase> objects = new JComboBox<>();
        objects.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                var resource = (MeshResourceBase) value;
                if (resource == null) {
                    return super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
                }
                var name = "%s - %s".formatted(uuidToString(resource.general().objectUUID()), resource.getType());
                if (resource instanceof StaticMeshResource mesh && mesh.meshDescription().isMoss()) {
                    name += " (moss)";
                }
                return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
            }
        });

        groups.addItemListener(e -> {
            var group = groups.getItemAt(groups.getSelectedIndex());
            if (group == null) {
                return;
            }
            objects.setModel(new DefaultComboBoxModel<>(loadResources(game, group.groupID())));
            objects.setSelectedItem(null);
        });

        objects.addItemListener(e -> {
            var object = objects.getItemAt(objects.getSelectedIndex());
            if (object == null) {
                return;
            }
            var scene = Converter.convert(object, game, Node.class)
                .map(Scene::of)
                .orElse(null);
            viewport.setScene(scene);
        });

        groups.setSelectedIndex(-1);
        objects.setSelectedIndex(-1);

        JPanel panel = new JPanel(new MigLayout("ins panel,wrap", "[][grow,fill]"));
        panel.add(new JLabel("Group:"));
        panel.add(groups);
        panel.add(new JLabel("Resource:"));
        panel.add(objects);

        JFrame frame = new JFrame("Model Viewer");
        frame.add(panel, BorderLayout.NORTH);
        frame.add(viewport, BorderLayout.CENTER);
        frame.setSize(950, 850);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static String uuidToString(GGUUID gguuid) {
        var builder = new StringBuilder(32);
        var format = HexFormat.of();
        format.toHexDigits(builder, gguuid.data0());
        format.toHexDigits(builder, gguuid.data1());
        format.toHexDigits(builder, gguuid.data2());
        format.toHexDigits(builder, gguuid.data3());
        format.toHexDigits(builder, gguuid.data4());
        format.toHexDigits(builder, gguuid.data5());
        format.toHexDigits(builder, gguuid.data6());
        format.toHexDigits(builder, gguuid.data7());
        format.toHexDigits(builder, gguuid.data8());
        format.toHexDigits(builder, gguuid.data9());
        format.toHexDigits(builder, gguuid.data10());
        format.toHexDigits(builder, gguuid.data11());
        format.toHexDigits(builder, gguuid.data12());
        format.toHexDigits(builder, gguuid.data13());
        format.toHexDigits(builder, gguuid.data14());
        format.toHexDigits(builder, gguuid.data15());
        return builder.toString();
    }

    private static StreamingGroupData[] loadGroups(ForbiddenWestGame game) {
        var graph = game.getStreamingGraph();
        return graph.groups().stream()
            .filter(group -> graph.types(group).stream().anyMatch(type -> MeshResourceBase.class.isAssignableFrom(type.type())))
            .sorted(Comparator.comparingInt(StreamingGroupData::groupID))
            .toArray(StreamingGroupData[]::new);
    }

    private static MeshResourceBase[] loadResources(ForbiddenWestGame game, int groupId) {
        StreamingObjectReader.GroupResult result;

        try {
            result = game.getStreamingReader().readGroup(groupId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var objects = result.objects();
        return objects.stream()
            .map(HFWTypeReader.ObjectInfo::object)
            .filter(MeshResourceBase.class::isInstance)
            .map(MeshResourceBase.class::cast)
            .toArray(MeshResourceBase[]::new);
    }
}
