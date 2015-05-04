/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.ui;

import org.esa.snap.framework.ui.application.support.AbstractToolView;
import org.esa.snap.util.logging.BeamLogManager;
import org.esa.snap.utils.CollectionHelper;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

/**
 * Commodity model view for echoing and filtering application log messages.
 *
 * @author  Cosmin Cara
 */
public class ConsoleToolView extends AbstractToolView {

    public static final String ID = ConsoleToolView.class.getName();

    private static final String WARNING = "Warning";
    private static final String INFO = "Informational";
    private static final String ERROR = "Error";
    private static final String CLEAR = "Clear";
    private static final String ICON_PATH = "org/esa/s2tbx/ui/%s";
    private static final String COLUMNS[] = {"Timestamp","Message Type", "Message"};
    private static final int COLUMN_WIDTHS[] = { 50, 50, 800};
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private Handler logHandler;
    private JTable logTable;
    private DefaultTableModel logTableModel;

    Map<String, List<Level>> filterMasks;
    Map<Level, String> reverseFilterMask;
    Map<String, String> filterIcons;
    Map<String, RowFilter<DefaultTableModel, Object>> rowFilterMap;
    Level currentFilter;

    public ConsoleToolView() {
        this.filterMasks = new LinkedHashMap<String, List<Level>>();
        this.filterIcons = new LinkedHashMap<String, String>();
        this.reverseFilterMask = new HashMap<Level, String>() {{
            put(Level.FINE, INFO);
            put(Level.FINER, INFO);
            put(Level.FINEST, INFO);
            put(Level.INFO, INFO);
            put(Level.WARNING, WARNING);
            put(Level.SEVERE, ERROR);
        }};
        this.filterMasks.put(INFO, new ArrayList<Level>() {{ add(Level.INFO); add(Level.FINE); add(Level.FINER); add(Level.FINEST); }});
        this.filterIcons.put(INFO, String.format(ICON_PATH, "info.gif"));
        this.filterMasks.put(WARNING, new ArrayList<Level>() {{ add(Level.WARNING); }});
        this.filterIcons.put(WARNING, String.format(ICON_PATH, "warning.gif"));
        this.filterMasks.put(ERROR, new ArrayList<Level>() {{ add(Level.SEVERE); }});
        this.filterIcons.put(ERROR, String.format(ICON_PATH, "error.gif"));
        currentFilter = Level.ALL;
        rowFilterMap = new HashMap<String, RowFilter<DefaultTableModel, Object>>();
        createRowFilters();
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                final LogRecord copy = record;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        Throwable e = copy.getThrown();
                        if (e != null) {
                            logTableModel.insertRow(logTableModel.getRowCount(), new Object[]{
                                    dateFormat.format(new Date(copy.getMillis())), reverseFilterMask.get(copy.getLevel()), e
                            });
                        } else {
                            logTableModel.insertRow(logTableModel.getRowCount(), new Object[]{
                                    dateFormat.format(new Date(copy.getMillis())), reverseFilterMask.get(copy.getLevel()), copy.getMessage()
                            });
                        }
                        logTableModel.fireTableDataChanged();
                        logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount() - 1, logTable.getColumnCount() - 1, true));
                    }
                });
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        };
    }

    @Override
    protected JComponent createControl() {
        BorderLayout borderLayout = new BorderLayout();
        final JPanel consoleViewPanel = new JPanel(borderLayout);
        String data[][] = new String[0][3];
        logTableModel = new DefaultTableModel(data, COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int col){
                return false;
            }
        };
        logTable = new JTable(logTableModel);
        logTable.setShowGrid(false);
        TableColumnModel columnModel = logTable.getTableHeader().getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i]);
        }
        MultiRenderer cellRenderer = new MultiRenderer();
        cellRenderer.registerRenderer(Throwable.class, new ToolTipCellRenderer());
        logTable.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);
        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setPreferredSize(consoleViewPanel.getPreferredSize());
        JToolBar toolbar = new JToolBar("Toolbar", JToolBar.VERTICAL);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(logTableModel);
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return true;
            }
        });
        logTable.setRowSorter(sorter);
        for (String key : filterIcons.keySet()) {
            final JToggleButton button = createTextlessToolbarButton(filterIcons.get(key), true, key);
            toolbar.add(button);
        }
        toolbar.addSeparator();
        toolbar.add(createToolbarButton(String.format(ICON_PATH, "clear.png"), CLEAR));
        consoleViewPanel.add(toolbar, BorderLayout.WEST);
        consoleViewPanel.add(scrollPane, BorderLayout.CENTER);
        BeamLogManager.getSystemLogger().addHandler(logHandler);
        return consoleViewPanel;
    }

    @Override
    public void dispose() {
        BeamLogManager.getSystemLogger().removeHandler(logHandler);
        super.dispose();
    }

    private JToggleButton createTextlessToolbarButton(String iconPath, boolean pressed, final String messageLevel) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        ImageIcon icon = new ImageIcon(classLoader.getResource(iconPath));
        final JToggleButton button = new JToggleButton(icon, pressed);
        button.setActionCommand(messageLevel);
        button.addActionListener((ActionEvent e) -> {
            Container parent = button.getParent();
            List<String> filterKeys = new ArrayList<String>();
            for (Component component : CollectionHelper.where(parent.getComponents(), JToggleButton.class::isInstance)) {
                JToggleButton buttonComponent = (JToggleButton) component;
                if (buttonComponent.getModel().isSelected()) {
                    filterKeys.add(buttonComponent.getActionCommand());
                }
            }
            List<RowFilter<DefaultTableModel, Object>> filters = filterKeys.stream().map(rowFilterMap::get).collect(Collectors.toList());
            //noinspection unchecked
            ((TableRowSorter) logTable.getRowSorter()).setRowFilter(RowFilter.orFilter(filters));
        });
        return button;
    }

    private JButton createToolbarButton(String iconPath, final String command) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        ImageIcon icon = new ImageIcon(classLoader.getResource(iconPath));
        final JButton button = new JButton(command, icon);
        button.setActionCommand(command);
        button.addActionListener((ActionEvent e) -> logTableModel.setNumRows(0));
        return button;
    }

    private void createRowFilters() {
        rowFilterMap.put(INFO, new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include (RowFilter.Entry entry) {
                //noinspection SuspiciousMethodCalls
                //return filterMasks.get(INFO).contains(entry.getValue(1));
                return INFO.equals(entry.getStringValue(1));
            }
        });
        rowFilterMap.put(WARNING, new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include (RowFilter.Entry entry) {
                //noinspection SuspiciousMethodCalls
                //return filterMasks.get(WARNING).contains(entry.getValue(1));
                return WARNING.equals(entry.getStringValue(1));
            }
        });
        rowFilterMap.put(ERROR, new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include (RowFilter.Entry entry) {
                //noinspection SuspiciousMethodCalls
                //return filterMasks.get(ERROR).contains(entry.getValue(1));
                return ERROR.equals(entry.getStringValue(1));
            }
        });
    }

    class MultiRenderer implements TableCellRenderer {
        private TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        private Map<Class, TableCellRenderer> registeredRenderers = new HashMap<>();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer delegate = null;
            if (value != null) {
                delegate = getDelegate(value.getClass());
            }

            if (delegate == null) {
                delegate = defaultRenderer;
            }

            return delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        public void registerRenderer(Class type, TableCellRenderer renderer) {
            registeredRenderers.put(type, renderer);
        }

        private TableCellRenderer getDelegate(Class type) {
            TableCellRenderer delegate = null;
            while (type != null && delegate == null) {
                delegate = registeredRenderers.get(type);
                type = type.getSuperclass();
            }
            return delegate;
        }
    }

    class ToolTipCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int column) {
            JLabel c = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null && Throwable.class.isInstance(value)) {
                Throwable exception = (Throwable) value;
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
                c.setToolTipText("<html>" + stringWriter.getBuffer().toString().replace("\r\n", "<br>") + "</html>");
                c.setText(exception.getMessage());
                c.setForeground(Color.RED);
            }
            return c;
        }
    }

}
