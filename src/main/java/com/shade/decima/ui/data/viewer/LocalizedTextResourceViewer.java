package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.Objects;

@ValueViewerRegistration({
    @Type(name = "LocalizedTextResource", game = GameType.DS),
    @Type(name = "LocalizedTextResource", game = GameType.DSDC),
    @Type(name = "LocalizedTextResource", game = GameType.HZD)
})
public class LocalizedTextResourceViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        final JTable table = new JTable(new LanguageTableModel());
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(0).setMaxWidth(120);

        final JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());

        return pane;
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final RTTIObject value = (RTTIObject) Objects.requireNonNull(((CoreEditor) editor).getSelectedValue());
        final RTTITypeEnum languages = ((CoreEditor) editor).getInput().getProject().getTypeRegistry().find("ELanguage");

        final JTable table = (JTable) ((JScrollPane) component).getViewport().getView();
        final LanguageTableModel model = (LanguageTableModel) table.getModel();

        model.setInput(value, languages);
    }

    private static class LanguageTableModel extends AbstractTableModel {
        private RTTIObject object;
        private RTTITypeEnum language;

        public void setInput(@NotNull RTTIObject object, @NotNull RTTITypeEnum language) {
            this.object = object;
            this.language = language;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return object != null ? object.objs("Entries").length - 1 : 0;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Language";
                case 1 -> "Text";
                default -> null;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final HwLocalizedText entry = object.<RTTIObject[]>get("Entries")[rowIndex].cast();
            return switch (columnIndex) {
                case 0 -> language.valueOf(rowIndex + 1).name();
                case 1 -> entry.getText();
                default -> null;
            };
        }
    }
}
