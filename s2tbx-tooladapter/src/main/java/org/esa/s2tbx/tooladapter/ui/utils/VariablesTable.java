package org.esa.s2tbx.tooladapter.ui.utils;

import org.esa.beam.framework.gpf.descriptor.ParameterDescriptor;
import org.esa.beam.framework.gpf.descriptor.S2tbxParameterDescriptor;
import org.esa.beam.framework.gpf.descriptor.S2tbxSystemVariable;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * Created by ramonag on 2/20/2015.
 */
public class VariablesTable extends JTable {
    private static String[] columnNames = {"", "Key", "Value"};
    private List<S2tbxSystemVariable> variables;
    private MultiRenderer tableRenderer;

    public VariablesTable(List<S2tbxSystemVariable> variables) {
        this.variables = variables;
        tableRenderer = new MultiRenderer();
        setModel(new VariablesTableModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getColumnModel().getColumn(0).setPreferredWidth(27);
        getColumnModel().getColumn(1).setPreferredWidth(150);
        getColumnModel().getColumn(2).setPreferredWidth(250);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return tableRenderer;
    }

    @Override
    public TableCellEditor getCellEditor() {
        return tableRenderer;
    }

    class VariablesTableModel extends AbstractTableModel {

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return variables.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return false;
                case 1:
                    return variables.get(row).getKey();
                case 2:
                    return variables.get(row).getValue();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    variables.remove(variables.get(rowIndex));
                    break;
                case 1:
                    variables.get(rowIndex).setKey(aValue.toString());
                    break;
                case 2:
                    variables.get(rowIndex).setValue(aValue.toString());
                    break;
            }
        }
    }

    class MultiRenderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        private AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/beam/resources/images/icons/DeleteShapeTool16.gif"),
                false);

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            switch (column){
                case 0: return delButton;
                default: return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            switch (column){
                case 0: return delButton;
                default: return getDefaultEditor(String.class).getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
