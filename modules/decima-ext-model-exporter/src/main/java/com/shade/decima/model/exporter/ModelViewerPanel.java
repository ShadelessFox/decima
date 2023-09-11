package com.shade.decima.model.exporter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatHelpButtonIcon;
import com.shade.decima.model.exporter.isr.SceneSerializer;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.decima.model.viewer.RenderLoop;
import com.shade.decima.model.viewer.camera.FirstPersonCamera;
import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

public class ModelViewerPanel extends JComponent implements Disposable, PropertyChangeListener {
    private static final Logger log = LoggerFactory.getLogger(ModelViewerPanel.class);

    private final JButton exportButton;
    private final JCheckBox embeddedBuffersCheckBox;
    private final JCheckBox exportTextures;
    private final JCheckBox embeddedTexturesCheckBox;
    private final JCheckBox useInstancingCheckBox;
    private final JCheckBox exportLodsCheckBox;
    private final JComboBox<ModelExporterProvider> exportersCombo;

    private JToolBar actionToolbar;
    private MeshViewerCanvas canvas;
    private RenderLoop loop;

    private ValueController<RTTIObject> controller;

    public ModelViewerPanel() {
        final ModelExporterProvider[] modelExporterProviders = ServiceLoader.load(ModelExporterProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .toArray(ModelExporterProvider[]::new);

        exportersCombo = new JComboBox<>(modelExporterProviders);
        exportersCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ModelExporterProvider> list, @NotNull ModelExporterProvider value, int index, boolean selected, boolean focused) {
                append("%s File".formatted(value.getExtension().toUpperCase()), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (.%s)".formatted(value.getExtension()), TextAttributes.GRAYED_ATTRIBUTES);
            }
        });
        exportButton = new JButton("Export\u2026");
        exportButton.setEnabled(false);
        exportButton.addActionListener(event -> {
            final ModelExporterProvider provider = exportersCombo.getItemAt(exportersCombo.getSelectedIndex());
            final JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(IOUtils.getBasename(controller.getEditor().getInput().getName()) + "." + provider.getExtension()));
            chooser.setDialogTitle("Choose output file");
            chooser.setFileFilter(new FileExtensionFilter(provider.getName(), provider.getExtension()));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            final Path output = chooser.getSelectedFile().toPath();
            final Boolean done = ProgressDialog.showProgressDialog(JOptionPane.getRootFrame(), "Export models", monitor -> {
                try {
                    export(monitor, output);
                    return true;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null);

            if (done == Boolean.TRUE) {
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Done");
            } else {
                IOUtils.unchecked(() -> Files.deleteIfExists(output));
            }
        });

        exportLodsCheckBox = new JCheckBox("Export LODs", false);
        useInstancingCheckBox = new JCheckBox("Use mesh instancing", true);
        embeddedBuffersCheckBox = new JCheckBox("Embed buffers", true);
        embeddedTexturesCheckBox = new JCheckBox("Embed textures", true);
        embeddedTexturesCheckBox.setEnabled(false);

        exportTextures = new JCheckBox("Export textures", false);
        exportTextures.addItemListener(e -> embeddedTexturesCheckBox.setEnabled(exportTextures.isSelected()));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new MigLayout("ins panel,gap 0", "[grow,fill]"));
        settingsPanel.setBorder(new LabeledBorder("Options"));
        settingsPanel.add(exportLodsCheckBox, "wrap");
        settingsPanel.add(useInstancingCheckBox, "wrap");
        settingsPanel.add(exportTextures, "wrap");
        settingsPanel.add(embeddedTexturesCheckBox, "wrap");
        settingsPanel.add(embeddedBuffersCheckBox, "wrap");

        final JToolBar exportToolbar = new JToolBar();
        exportToolbar.setBorder(null);
        exportToolbar.add(new AbstractAction(null, new FlatHelpButtonIcon()) {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    Desktop.getDesktop().browse(URI.create("https://github.com/ShadelessFox/decima/wiki/Model-export"));
                } catch (IOException e) {
                    UIUtils.showErrorDialog(e, "Unable to open wiki page");
                }
            }
        });

        final JPanel exporterPanel = new JPanel();
        exporterPanel.setLayout(new MigLayout("ins panel,gap 0", "[grow,fill]"));
        exporterPanel.add(new JLabel("Output format:"), "wrap");
        exporterPanel.add(exportersCombo, "grow x,split");
        exporterPanel.add(exportToolbar, "gapx 0,wrap");
        exporterPanel.add(settingsPanel, "wrap");
        exporterPanel.add(exportButton);

        setLayout(new BorderLayout());
        add(exporterPanel, BorderLayout.SOUTH);

        try {
            canvas = new MeshViewerCanvas(new FirstPersonCamera());
            canvas.setPreferredSize(new Dimension(400, 400));
            canvas.setMinimumSize(new Dimension(100, 100));
            canvas.addPropertyChangeListener(this);
        } catch (Throwable e) {
            log.error("Can't create GL canvas: " + e.getMessage());
        }

        if (canvas != null) {
            final JLabel statusLabel = new JLabel();
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

            actionToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_MODEL_VIEWER_ID, key -> switch (key) {
                case "canvas" -> canvas;
                default -> null;
            });
            actionToolbar.add(Box.createHorizontalGlue());
            actionToolbar.add(statusLabel);

            final JPanel canvasHolder = new JPanel();
            canvasHolder.setLayout(new BorderLayout());
            canvasHolder.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.shadow")));
            canvasHolder.add(canvas, BorderLayout.CENTER);

            add(actionToolbar, BorderLayout.NORTH);
            add(canvasHolder, BorderLayout.CENTER);

            loop = new RenderLoop(JOptionPane.getRootFrame(), canvas) {
                private long renderTime;
                private long updateTime;
                private long framesPassed;

                @Override
                public void beforeRender() {
                    renderTime = System.currentTimeMillis();
                }

                @Override
                public void afterRender() {
                    framesPassed += 1;

                    if (renderTime - updateTime >= 1000) {
                        statusLabel.setText("%.3f ms/frame, %d fps".formatted(1000.0 / framesPassed, framesPassed));
                        updateTime = renderTime;
                        framesPassed = 0;
                    }
                }
            };

            loop.start();
        } else {
            final JLabel placeholder = new JLabel("Preview is not supported");
            placeholder.setHorizontalAlignment(SwingConstants.CENTER);
            placeholder.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

            add(placeholder, BorderLayout.CENTER);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final String name = event.getPropertyName();

        if (name.equals("background")) {
            MenuManager.getInstance().update(actionToolbar);
        }
    }

    @Override
    public void dispose() {
        if (canvas != null) {
            loop.dispose();
            canvas.dispose();
        }
    }

    public void setInput(@Nullable ValueController<RTTIObject> controller) {
        this.controller = controller;
        this.exportButton.setEnabled(controller != null);

        if (canvas != null) {
            Node node = null;

            if (controller != null) {
                try {
                    node = SceneSerializer.serialize(controller);
                } catch (Exception e) {
                    log.debug("Can't load preview for model of type {}", controller.getValueType(), e);
                }
            }

            if (node != null) {
                canvas.setModel(new NodeModel(node));
            }
        }
    }

    private void export(@NotNull ProgressMonitor monitor, @NotNull Path output) throws Throwable {
        final var provider = exportersCombo.getItemAt(exportersCombo.getSelectedIndex());
        final var object = controller.getValue();

        final var settings = new ExportSettings(exportTextures.isSelected(), embeddedTexturesCheckBox.isSelected(), exportLodsCheckBox.isSelected(), useInstancingCheckBox.isSelected(), embeddedBuffersCheckBox.isSelected());
        final var exporter = provider.create(controller.getProject(), settings, output.getParent());
        final var name = IOUtils.getBasename(output.getFileName().toString());

        try (ProgressMonitor.Task task = monitor.begin("Exporting %s".formatted(name), 2)) {
            try (Writer writer = Files.newBufferedWriter(output)) {
                exporter.export(task.split(1), controller.getBinary(), object, name, writer);
            }
        }
    }
}
