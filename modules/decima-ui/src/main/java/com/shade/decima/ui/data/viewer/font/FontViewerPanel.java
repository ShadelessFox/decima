package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.ds.DSFont;
import com.shade.decima.model.rtti.types.hzd.HZDFont;
import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class FontViewerPanel extends JComponent {
    public static final DataKey<HwFont> FONT_KEY = new DataKey<>("font", HwFont.class);

    private final GlyphPanel glyphPanel;
    private final JComboBox<HwFont.Glyph> glyphCombo;
    private final JCheckBox showMetricsCheckbox;

    private final JToolBar bottomToolbar;

    private HwFont font;

    public FontViewerPanel() {
        this.glyphPanel = new GlyphPanel();
        this.glyphPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.shadow")));

        this.glyphCombo = new JComboBox<>();
        this.glyphCombo.addItemListener(e -> glyphPanel.setIndex(glyphCombo.getSelectedIndex()));
        this.glyphCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends HwFont.Glyph> list, HwFont.Glyph value, int index, boolean selected, boolean focused) {
                if (value != null) {
                    append("U+%04X ".formatted(value.getCodePoint()), TextAttributes.GRAYED_ATTRIBUTES);
                    append(" \"%c\"".formatted(value.getCodePoint()), TextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    append("No characters", TextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        });

        final DataContext context = key -> switch (key) {
            case "font" -> font;
            default -> null;
        };

        final JToolBar toolbar = new JToolBar();
        toolbar.add(new JLabel("Character: "));
        toolbar.add(glyphCombo);

        this.showMetricsCheckbox = new JCheckBox("Show metrics");
        this.showMetricsCheckbox.addItemListener(e -> glyphPanel.setShowDetails(showMetricsCheckbox.isSelected()));

        this.bottomToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_FONT_VIEWER_BOTTOM_ID, context);
        this.bottomToolbar.add(Box.createHorizontalGlue());
        this.bottomToolbar.add(showMetricsCheckbox);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(glyphPanel, BorderLayout.CENTER);
        add(bottomToolbar, BorderLayout.SOUTH);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 0);
    }

    public void setInput(@NotNull RTTIObject object, @NotNull GameType game) {
        this.font = game == GameType.HZD ? new HZDFont(object) : new DSFont(object);
        this.glyphPanel.setInput(font);
        this.glyphCombo.setModel(new FontModel(font));
        this.glyphCombo.setSelectedItem(null);

        if (font.getGlyphCount() > 0) {
            glyphCombo.setSelectedItem(font.getGlyph(0));
        }

        MenuManager.getInstance().update(bottomToolbar);
    }

    private static class FontModel extends AbstractListModel<HwFont.Glyph> implements ComboBoxModel<HwFont.Glyph> {
        private final HwFont font;
        private HwFont.Glyph selection;

        public FontModel(@NotNull HwFont font) {
            this.font = font;
        }

        @Override
        public int getSize() {
            return font.getGlyphCount();
        }

        @Override
        public HwFont.Glyph getElementAt(int index) {
            return font.getGlyph(index);
        }

        @Override
        public void setSelectedItem(Object item) {
            if (!Objects.equals(item, selection)) {
                selection = (HwFont.Glyph) item;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public Object getSelectedItem() {
            return selection;
        }
    }
}
