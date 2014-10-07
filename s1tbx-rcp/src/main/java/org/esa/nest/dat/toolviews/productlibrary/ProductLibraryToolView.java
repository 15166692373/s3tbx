/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dat.toolviews.productlibrary;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.CheckListDialog;
import org.esa.snap.dat.dialogs.BatchGraphDialog;
import org.esa.nest.dat.toolviews.Projects.Project;
import org.esa.nest.dat.toolviews.productlibrary.model.*;
import org.esa.nest.dat.toolviews.productlibrary.timeline.TimelinePanel;
import org.esa.nest.dat.utils.FileFolderUtils;
import org.esa.nest.dat.utils.ProductOpener;
import org.esa.snap.db.DBQuery;
import org.esa.snap.db.DBScanner;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.util.ClipboardUtils;
import org.esa.snap.util.ResourceUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class ProductLibraryToolView extends AbstractToolView {

    private static final ImageIcon updateIcon = UIUtils.loadImageIcon("icons/Update24.gif");
    private static final ImageIcon updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
    private static final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
    private static final ImageIcon stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);

    private JPanel mainPanel;
    private JComboBox repositoryListCombo;
    private JTable productEntryTable;
    private SortingDecorator sortedModel;

    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton addToProjectButton;
    private JButton openAllSelectedButton;
    private JButton batchProcessButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton updateButton;

    private JMenuItem copyToItem;
    private JMenuItem moveToItem;
    private JMenuItem deleteItem;

    private LabelBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private File currentDirectory;
    private ProductOpener openHandler;
    private ProductLibraryConfig libConfig;
    private static final String helpId = "productLibrary";

    private WorldMapUI worldMapUI = null;
    private DatabasePane dbPane;
    private final MyDatabaseQueryListener dbQueryListener = new MyDatabaseQueryListener();

    public ProductLibraryToolView() {
    }

    @Override
    public void componentOpened() {
        dbPane.getDB();
    }

    public JComponent createControl() {

        libConfig = new ProductLibraryConfig(VisatApp.getApp().getPreferences());
        openHandler = new ProductOpener(VisatApp.getApp());

        initUI();
        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(final ComponentEvent e) {
                if (progMon != null)
                    progMon.setCanceled(true);
            }
        });
        applyConfig(libConfig);
        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }
        });
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

        return mainPanel;
    }

    private void applyConfig(final ProductLibraryConfig config) {
        final File[] baseDirList = config.getBaseDirs();
        repositoryListCombo.insertItemAt(DBQuery.ALL_FOLDERS, 0);
        for (File f : baseDirList) {
            repositoryListCombo.insertItemAt(f, repositoryListCombo.getItemCount());
        }
        if (baseDirList.length > 0)
            repositoryListCombo.setSelectedIndex(0);
    }

    private void performSelectAction() {
        updateStatusLabel();
        final ProductEntry[] selections = getSelectedProductEntries();
        setOpenProductButtonsEnabled(selections.length > 0);

        updateContextMenu(selections);
        dbPane.updateProductSelectionText(selections);
        worldMapUI.setSelectedProductEntryList(selections);
    }

    private void updateContextMenu(final ProductEntry[] selections) {
        boolean allValid = true;
        for (ProductEntry entry : selections) {
            if (!ProductFileHandler.canMove(entry)) {
                allValid = false;
                break;
            }
        }
        copyToItem.setEnabled(allValid);
        moveToItem.setEnabled(allValid);
        deleteItem.setEnabled(allValid);
    }

    private void performOpenAction() {
        if (openHandler != null) {
            openHandler.openProducts(getSelectedFiles());
        }
    }

    /**
     * Copy the selected file list to the clipboard
     */
    private void performCopyAction() {
        final File[] fileList = getSelectedFiles();
        if (fileList.length != 0)
            ClipboardUtils.copyToClipboard(fileList);
    }

    private void performCopyToAction() {
        final File targetFolder = promptForRepositoryBaseDir();
        if (targetFolder == null) return;

        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.copyTo(entry, targetFolder);
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to copy file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
    }

    private void performMoveToAction() {
        final File targetFolder = promptForRepositoryBaseDir();
        if (targetFolder == null) return;

        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.moveTo(entry, targetFolder);
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to move file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
        rescanFolder();
        UpdateUI();
    }

    private void performDeleteAction() {
        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.delete(entry);

            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to delete file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
        rescanFolder();
        UpdateUI();
    }

    private ProductEntry[] getSelectedProductEntries() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final ProductEntry[] selectedEntries = new ProductEntry[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedEntries[i] = (ProductEntry) entry;
            }
        }
        return selectedEntries;
    }

    private File[] getSelectedFiles() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final File[] selectedFiles = new File[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedFiles[i] = ((ProductEntry) entry).getFile();
            }
        }
        return selectedFiles;
    }

    private JPopupMenu createEntryTablePopup() {
        final JPopupMenu popup = new JPopupMenu();
        final JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                productEntryTable.selectAll();
                performSelectAction();
            }
        });
        popup.add(selectAllItem);

        final JMenuItem openSelectedItem = new JMenuItem("Open Selected");
        openSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });
        popup.add(openSelectedItem);

        final JMenuItem copySelectedItem = new JMenuItem("Copy Selected");
        copySelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyAction();
            }
        });
        popup.add(copySelectedItem);

        popup.addSeparator();

        copyToItem = new JMenuItem("Copy Selected Files To...");
        copyToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyToAction();
            }
        });
        popup.add(copyToItem);

        moveToItem = new JMenuItem("Move Selected Files To...");
        moveToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performMoveToAction();
            }
        });
        popup.add(moveToItem);

        deleteItem = new JMenuItem("Delete Selected Files");
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int status = VisatApp.getApp().showQuestionDialog("Are you sure you want to delete these products", "");
                if (status == JOptionPane.YES_OPTION)
                    performDeleteAction();
            }
        });
        popup.add(deleteItem);

        final JMenuItem exploreItem = new JMenuItem("Browse Folder");
        exploreItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Point pos = productEntryTable.getMousePosition();
                int row = 0;
                if (pos != null)
                    row = productEntryTable.rowAtPoint(pos);
                final Object entry = productEntryTable.getValueAt(row, 0);
                if (entry != null && entry instanceof ProductEntry) {
                    final ProductEntry prodEntry = (ProductEntry) entry;
                    try {
                        Desktop.getDesktop().open(prodEntry.getFile().getParentFile());
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        });
        popup.add(exploreItem);

        popup.addSeparator();

        final JMenu sortMenu = new JMenu("Sort By");
        popup.add(sortMenu);

        sortMenu.add(createSortItem("Product Name", SortingDecorator.SORT_BY.NAME));
        sortMenu.add(createSortItem("Product Type", SortingDecorator.SORT_BY.TYPE));
        sortMenu.add(createSortItem("Acquisition Date", SortingDecorator.SORT_BY.DATE));
        sortMenu.add(createSortItem("Mission", SortingDecorator.SORT_BY.MISSON));
        sortMenu.add(createSortItem("File Size", SortingDecorator.SORT_BY.FILESIZE));

        return popup;
    }

    private JMenuItem createSortItem(final String name, final SortingDecorator.SORT_BY sortBy) {
        final JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                sortedModel.sortBy(sortBy);
            }
        });
        return item;
    }

    private JPopupMenu createGraphPopup() {
        final File graphPath = ResourceUtils.getGraphFolder("");

        final JPopupMenu popup = new JPopupMenu();
        if (graphPath.exists()) {
            createGraphMenu(popup, graphPath);
        }
        return popup;
    }

    private void createGraphMenu(final JPopupMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        if (batchProcessButton.isEnabled())
                            batchProcess(getSelectedProductEntries(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    private void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        if (batchProcessButton.isEnabled())
                            batchProcess(getSelectedProductEntries(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    private static void batchProcess(final ProductEntry[] productEntryList, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(VisatApp.getApp(),
                "Batch Processing", "batchProcessing", false);
        batchDlg.setInputFiles(productEntryList);
        if (graphFile != null) {
            batchDlg.LoadGraphFile(graphFile);
        }
        batchDlg.show();
    }

    private void addRepository() {
        final File baseDir = promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        final Map<String, Boolean> checkBoxMap = new HashMap<>(3);
        checkBoxMap.put("Generate quicklooks?", false);
        checkBoxMap.put("Search folder recursively?", true);

        final CheckListDialog dlg = new CheckListDialog("Scan Folder Options", checkBoxMap);
        dlg.show();

        final boolean doRecursive = checkBoxMap.get("Search folder recursively?");
        final boolean doQuicklooks = checkBoxMap.get("Generate quicklooks?");

        libConfig.addBaseDir(baseDir);
        final int index = repositoryListCombo.getItemCount();
        repositoryListCombo.insertItemAt(baseDir, index);
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

        updateRepostitory(baseDir, doRecursive, doQuicklooks);
    }

    private void updateRepostitory(final File baseDir, final boolean doRecursive, final boolean doQuicklooks) {
        if (baseDir == null) return;
        progMon = new LabelBarProgressMonitor(progressBar, statusLabel);
        progMon.addListener(new MyProgressBarListener());
        final DBScanner scanner = new DBScanner(dbPane.getDB(), baseDir, doRecursive, doQuicklooks, progMon);
        scanner.addListener(new MyDatabaseScannerListener());
        scanner.execute();
    }

    private void removeRepository() {

        final Object selectedItem = repositoryListCombo.getSelectedItem();
        final int index = repositoryListCombo.getSelectedIndex();
        if (index == 0) {
            final int status = VisatApp.getApp().showQuestionDialog("This will remove all folders and products from the database.\n" +
                    "Are you sure you wish to continue?", null);
            if (status == JOptionPane.NO_OPTION)
                return;
            while (repositoryListCombo.getItemCount() > 1) {
                final File baseDir = (File) repositoryListCombo.getItemAt(1);
                libConfig.removeBaseDir(baseDir);
                repositoryListCombo.removeItemAt(1);
                dbPane.removeProducts(baseDir);
            }
            try {
                dbPane.getDB().removeAllProducts();
            } catch (Exception e) {
                System.out.println("Failed to remove all products");
            }
        } else if (selectedItem instanceof File) {
            final File baseDir = (File) selectedItem;
            final int status = VisatApp.getApp().showQuestionDialog("This will remove all products within " +
                    baseDir.getAbsolutePath() + " from the database\n" +
                    "Are you sure you wish to continue?", null);
            if (status == JOptionPane.NO_OPTION)
                return;
            libConfig.removeBaseDir(baseDir);
            repositoryListCombo.removeItemAt(index);
            dbPane.removeProducts(baseDir);
        }
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);
        UpdateUI();
    }

    private void setUIComponentsEnabled(final boolean enable) {
        removeButton.setEnabled(enable);
        updateButton.setEnabled(enable);
        repositoryListCombo.setEnabled(enable);
    }

    private void setOpenProductButtonsEnabled(final boolean enable) {
        addToProjectButton.setEnabled(enable);
        openAllSelectedButton.setEnabled(enable);
        batchProcessButton.setEnabled(enable);
    }

    private void toggleUpdateButton(final String command) {
        if (command.equals(LabelBarProgressMonitor.stopCommand)) {
            updateButton.setIcon(stopIcon);
            updateButton.setRolloverIcon(stopRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.stopCommand);
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            updateButton.setIcon(updateIcon);
            updateButton.setRolloverIcon(updateRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
        }
    }

    private File promptForRepositoryBaseDir() {
        final JFileChooser fileChooser = createDirectoryChooser();
        fileChooser.setCurrentDirectory(currentDirectory);
        final int response = fileChooser.showOpenDialog(mainPanel);
        currentDirectory = fileChooser.getCurrentDirectory();
        File selectedDir = fileChooser.getSelectedFile();
        if (selectedDir != null && selectedDir.isFile())
            selectedDir = selectedDir.getParentFile();
        if (response == JFileChooser.APPROVE_OPTION) {
            return selectedDir;
        }
        return null;
    }

    private static JFileChooser createDirectoryChooser() {
        final JFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File f) {
                return true;//f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories"; /* I18N */
            }
        });
        fileChooser.setDialogTitle("Select Directory"); /* I18N */
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setApproveButtonText("Select"); /* I18N */
        fileChooser.setApproveButtonMnemonic('S');
        return fileChooser;
    }

    private void initUI() {

        dbPane = new DatabasePane();
        dbPane.addListener(dbQueryListener);

        final JPanel northPanel = createHeaderPanel();
        final JPanel southPanel = createButtonPanel();

        final DatabaseStatistics stats = new DatabaseStatistics(dbPane);
        final TimelinePanel timeLinePanel = new TimelinePanel(stats);
        dbPane.addListener(timeLinePanel);
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createCentrePanel(), timeLinePanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.99);

        mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private JPanel createHeaderPanel() {
        final JPanel headerBar = new JPanel();
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        updateButton = createToolButton("updateButton", updateIcon);
        updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    progMon.setCanceled(true);
                } else {
                    rescanFolder();
                }
            }
        });
        headerBar.add(updateButton, gbc);

        headerBar.add(new JLabel("Folder:")); /* I18N */
        gbc.weightx = 99;
        repositoryListCombo = new JComboBox();
        setComponentName(repositoryListCombo, "repositoryListCombo");
        repositoryListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    final Object selectedItem = repositoryListCombo.getSelectedItem();
                    if (selectedItem instanceof File) {
                        dbPane.setBaseDir((File) selectedItem);
                    } else {
                        dbPane.setBaseDir(null);
                    }
                }
            }
        });
        headerBar.add(repositoryListCombo, gbc);
        gbc.weightx = 0;

        addButton = createToolButton("addButton", UIUtils.loadImageIcon("icons/Plus24.gif"));
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                addRepository();
            }
        });
        headerBar.add(addButton, gbc);

        removeButton = createToolButton("removeButton", UIUtils.loadImageIcon("icons/Minus24.gif"));
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeRepository();
            }
        });
        headerBar.add(removeButton, gbc);

        final JButton helpButton = createToolButton("helpButton", UIUtils.loadImageIcon("icons/Help24.gif"));
        setComponentName(helpButton, "helpButton");
        HelpSys.enableHelpOnButton(helpButton, helpId);
        headerBar.add(helpButton, gbc);

        return headerBar;
    }

    private JPanel createButtonPanel() {
        addToProjectButton = new JButton();
        setComponentName(addToProjectButton, "addToProject");
        addToProjectButton.setText("Import to Project");
        addToProjectButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Project.instance().ImportFileList(getSelectedFiles());
            }
        });

        openAllSelectedButton = new JButton();
        setComponentName(openAllSelectedButton, "openAllSelectedButton");
        openAllSelectedButton.setText("Open Selected");
        openAllSelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });

        batchProcessButton = new JButton();
        setComponentName(batchProcessButton, "batchProcessButton");
        batchProcessButton.setText("Batch Process");
        batchProcessButton.setToolTipText("Right click to select a graph");
        batchProcessButton.setComponentPopupMenu(createGraphPopup());
        batchProcessButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                batchProcess(getSelectedProductEntries(), null);
            }
        });

        final JPanel openPanel = new JPanel(new BorderLayout(4, 4));
        openPanel.add(addToProjectButton, BorderLayout.WEST);
        openPanel.add(openAllSelectedButton, BorderLayout.CENTER);
        openPanel.add(batchProcessButton, BorderLayout.EAST);

        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        statusLabel = new JLabel("");
        statusLabel.setMinimumSize(new Dimension(100, 10));
        southPanel.add(statusLabel, BorderLayout.CENTER);
        southPanel.add(openPanel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        setComponentName(progressBar, "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.EAST);

        return southPanel;
    }

    private JPanel createCentrePanel() {

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(200, 577));
        leftPanel.add(dbPane, BorderLayout.NORTH);

        productEntryTable = new JTable();
        productEntryTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        productEntryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productEntryTable.setComponentPopupMenu(createEntryTablePopup());
        productEntryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    performOpenAction();
                } else if (clickCount == 1) {
                    performSelectAction();
                }
            }
        });


        final JScrollPane tablePane = new JScrollPane(productEntryTable);
        tablePane.setMinimumSize(new Dimension(400, 400));

        worldMapUI = new WorldMapUI();
        worldMapUI.addListener(dbQueryListener);

        final JSplitPane splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePane, worldMapUI.getWorlMapPane());
        splitPaneV.setOneTouchExpandable(true);
        splitPaneV.setResizeWeight(0.8);

        final JPanel centrePanel = new JPanel(new BorderLayout());
        centrePanel.add(leftPanel, BorderLayout.WEST);
        centrePanel.add(splitPaneV, BorderLayout.CENTER);

        return centrePanel;
    }

    private void setComponentName(JComponent button, String name) {
        button.setName(getClass().getName() + name);
    }

    public void UpdateUI() {
        dbPane.refresh();
        productEntryTable.updateUI();
    }

    private void rescanFolder() {
        if (repositoryListCombo.getSelectedIndex() != 0) {
            updateRepostitory((File) repositoryListCombo.getSelectedItem(), true, true);
        } else {
            final File[] baseDirList = libConfig.getBaseDirs();
            for (File f : baseDirList) {
                updateRepostitory(f, true, true);
            }
        }
    }

    private JButton createToolButton(final String name, final ImageIcon icon) {
        final JButton button = (JButton) ToolButtonFactory.createButton(icon, false);
        setComponentName(button, name);
        //button.setBackground(mainPanel.getBackground());
        return button;
    }

    private void updateStatusLabel() {
        String selectedText = "";
        final int selecteRows = productEntryTable.getSelectedRowCount();

        setOpenProductButtonsEnabled(selecteRows > 0);
        if (selecteRows > 0)
            selectedText = ", " + selecteRows + " Selected";
        else
            dbPane.updateProductSelectionText(null);
        statusLabel.setText(productEntryTable.getRowCount() + " Products" + selectedText);
    }

    public void ShowRepository(final ProductEntry[] productEntryList) {
        final ProductEntryTableModel tableModel = new ProductEntryTableModel(productEntryList);
        sortedModel = new SortingDecorator(tableModel, productEntryTable.getTableHeader());
        productEntryTable.setModel(sortedModel);
        productEntryTable.setColumnModel(tableModel.getColumnModel());
        updateStatusLabel();
        worldMapUI.setProductEntryList(productEntryList);
        worldMapUI.setSelectedProductEntryList(null);
    }

    private static void handleErrorList(final java.util.List<DBScanner.ErrorFile> errorList) {
        final StringBuilder str = new StringBuilder();
        int cnt = 1;
        for (DBScanner.ErrorFile err : errorList) {
            str.append(err.message);
            str.append("   ");
            str.append(err.file.getAbsolutePath());
            str.append('\n');
            if (cnt >= 20) {
                str.append("plus " + (errorList.size() - 20) + " other errors...\n");
                break;
            }
            ++cnt;
        }
        final String question = "\nWould you like to save the list to a text file?";
        if (VisatApp.getApp().showQuestionDialog("Product Errors",
                "The follow files have errors:\n" + str.toString() + question,
                null) == 0) {

            File file = FileFolderUtils.GetSaveFilePath("Save as...", "Text", "txt",
                    "ProductErrorList", "Products with errors");
            try {
                writeErrors(errorList, file);
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to save to " + file.getAbsolutePath());
                file = FileFolderUtils.GetSaveFilePath("Save as...", "Text", "txt",
                        "ProductErrorList", "Products with errors");
                try {
                    writeErrors(errorList, file);
                } catch (Exception ignore) {
                    //
                }
            }
            if (Desktop.isDesktopSupported() && file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private static void writeErrors(final java.util.List<DBScanner.ErrorFile> errorList, final File file) throws Exception {
        if (file == null) return;

        PrintStream p = null; // declare a print stream object
        try {
            final FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
            // Connect print stream to the output stream
            p = new PrintStream(out);

            for (DBScanner.ErrorFile err : errorList) {
                p.println(err.message + "   " + err.file.getAbsolutePath());
            }
        } finally {
            if (p != null)
                p.close();
        }
    }

    private class MyDatabaseQueryListener implements DatabaseQueryListener {

        public void notifyNewEntryListAvailable() {
            ShowRepository(dbPane.getProductEntryList());
        }

        public void notifyNewMapSelectionAvailable() {
            dbPane.setSelectionRect(worldMapUI.getSelectionBox());
        }
    }

    private class MyDatabaseScannerListener implements DBScanner.DBScannerListener {

        public void notifyMSG(final DBScanner dbScanner, final MSG msg) {
            if (msg.equals(DBScanner.DBScannerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = dbScanner.getErrorList();
                if (!errorList.isEmpty()) {
                    handleErrorList(errorList);
                }
            }
            UpdateUI();
        }
    }

    private class MyProgressBarListener implements LabelBarProgressMonitor.ProgressBarListener {
        public void notifyStart() {
            progressPanel.setVisible(true);
            toggleUpdateButton(LabelBarProgressMonitor.stopCommand);
        }

        public void notifyDone() {
            progressPanel.setVisible(false);
            toggleUpdateButton(LabelBarProgressMonitor.updateCommand);
            updateButton.setEnabled(true);
            mainPanel.setCursor(Cursor.getDefaultCursor());
        }
    }

}