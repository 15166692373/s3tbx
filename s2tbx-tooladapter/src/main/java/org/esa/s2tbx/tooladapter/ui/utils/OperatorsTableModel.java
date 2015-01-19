package org.esa.s2tbx.tooladapter.ui.utils;

import org.esa.beam.framework.gpf.OperatorSpi;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramonag on 1/16/2015.
 */
public class OperatorsTableModel extends AbstractTableModel {

    private String[] columnNames = {"", "Tool name", "Tool description"};
    private boolean[] toolsChecked = null;
    private List<OperatorSpi> data = null;

    public OperatorsTableModel(List<OperatorSpi> operators){
        this.data = operators;
        this.toolsChecked = new boolean[this.data.size()];
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(columnIndex == 0){
            return toolsChecked[rowIndex];
        } else {
            return data.get(rowIndex).getOperatorDescriptor().getName();
        }
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Class getColumnClass(int c) {
        if(c == 0){
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if(col == 0){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        this.toolsChecked[row] = (boolean)value;
    }

    public OperatorSpi getFirstCheckedOperator(){
        for (int i=0;i<this.toolsChecked.length;i++){
            if(this.toolsChecked[i]){
                return this.data.get(i);
            }
        }
        return null;
    }

    public List<OperatorSpi> getCheckedOperators(){
        List<OperatorSpi> result = new ArrayList<OperatorSpi>();
        for (int i=0;i<this.toolsChecked.length;i++){
            if(this.toolsChecked[i]){
                result.add(this.data.get(i));
            }
        }
        return null;
    }
}
