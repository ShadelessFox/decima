package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.impl.LocalizedTextResourceMessageHandler.LanguageEntry;
import com.shade.decima.model.rtti.objects.Language;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.Objects;

@ValueViewerRegistration(@Type(name = "LocalizedTextResource", game = GameType.DS))
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
        final RTTIObject value = (RTTIObject) ((CoreEditor) editor).getSelectedValue();
        final JTable table = (JTable) ((JScrollPane) component).getViewport().getView();
        final LanguageTableModel model = (LanguageTableModel) table.getModel();

        model.setInput(Objects.requireNonNull(value));
    }

    private static class LanguageTableModel extends AbstractTableModel {
        private RTTIObject object;

        public void setInput(@NotNull RTTIObject object) {
            this.object = object;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return Language.values().length - 1;
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
            final LanguageEntry entry = object.<RTTIObject[]>get("Entries")[rowIndex].cast();
            return switch (columnIndex) {
                case 0 -> Language.values()[rowIndex + 1].getLabel();
                case 1 -> entry.text;
                default -> null;
            };
        }
    }
}
