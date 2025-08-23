package com.shade.decima.ui.data.viewer;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwLocalizedText;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

@ValueViewerRegistration({
    @Selector(type = @Type(name = "LocalizedTextResource")),
})
public class LocalizedTextResourceViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        final JTable table = new JTable(new LanguageTableModel());
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(0).setMaxWidth(120);
        table.setPreferredSize(new Dimension(400, 300));

        return UIUtils.createBorderlessScrollPane(table);
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final RTTIObject object = (RTTIObject) controller.getValue();
        final JTable table = (JTable) ((JScrollPane) component).getViewport().getView();
        final LanguageTableModel model = (LanguageTableModel) table.getModel();

        model.setInput(object.obj("Data").cast());
    }

    private static class LanguageTableModel extends AbstractTableModel {
        private HwLocalizedText text;

        public void setInput(@NotNull HwLocalizedText text) {
            this.text = text;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return text != null ? text.getLocalizationCount() : 0;
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
            return switch (columnIndex) {
                case 0 -> text.getLanguage(rowIndex);
                case 1 -> text.getTranslation(rowIndex);
                default -> null;
            };
        }
    }
}
