package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class FontViewerPanel extends JComponent {
    private final GlyphPanel glyphPanel;
    private final JComboBox<RTTIObject> characterCombo;
    private final JCheckBox metricsCombo;

    private GameType game;

    public FontViewerPanel() {
        this.glyphPanel = new GlyphPanel();

        this.characterCombo = new JComboBox<>();
        this.characterCombo.addItemListener(e -> glyphPanel.setIndex(characterCombo.getSelectedIndex()));
        this.characterCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends RTTIObject> list, RTTIObject value, int index, boolean selected, boolean focused) {
                if (value != null) {
                    final int codePoint = game == GameType.HZD ? value.i16("Char") : value.i32("CodePoint");
                    append("U+%04X ".formatted(codePoint), TextAttributes.GRAYED_ATTRIBUTES);
                    append(" \"%c\"".formatted(codePoint), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("No characters", TextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        });

        this.metricsCombo = new JCheckBox("Show metrics");
        this.metricsCombo.addItemListener(e -> glyphPanel.setShowDetails(metricsCombo.isSelected()));

        final JToolBar toolbar = new JToolBar();
        toolbar.add(new JLabel("Character: "));
        toolbar.add(characterCombo);
        toolbar.add(metricsCombo);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(glyphPanel, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 0);
    }

    public void setObject(@NotNull RTTIObject object, @NotNull GameType game) {
        final RTTIObject data = object.obj("FontResourceData");
        final RTTIObject[] characters = data.objs(game == GameType.HZD ? "CharInfo" : "CodePointInfo");

        this.game = game;
        this.glyphPanel.setObject(object, game);
        this.characterCombo.setModel(new DefaultComboBoxModel<>(characters));
        this.characterCombo.setSelectedItem(null);

        if (characters.length > 0) {
            characterCombo.setSelectedItem(characters[0]);
        }
    }
}
