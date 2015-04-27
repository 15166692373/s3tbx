package org.esa.snap.ui.tooladapter.dialogs;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterConstants;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterIO;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterOp;
import org.esa.snap.framework.gpf.operators.tooladapter.ToolAdapterRegistry;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.UIUtils;
import org.esa.snap.framework.ui.tool.ToolButtonFactory;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.tooladapter.actions.ToolAdapterActionRegistrar;
import org.esa.snap.ui.tooladapter.model.OperatorsTableModel;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Dialog that allows the management (create, edit, remove and execute) of external
 * tool adapters
 *
 * @author Ramona Manda
 * @author Cosmin Cara
 */
@NbBundle.Messages({
        "ToolTipNewOperator_Text=Define new operator",
        "ToolTipCopyOperator_Text=Duplicate the selected operator",
        "ToolTipEditOperator_Text=Edit the selected operator",
        "ToolTipExecuteOperator_Text=Execute the selected operator",
        "ToolTipDeleteOperator_Text=Delete the selected operator(s)",
        "Icon_New=/org/esa/snap/resources/images/icons/New24.gif",
        "Icon_Copy=/org/esa/snap/resources/images/icons/Copy24.gif",
        "Icon_Edit=/org/esa/snap/resources/images/icons/Edit24.gif",
        "Icon_Execute=/org/esa/snap/resources/images/icons/Update24.gif",
        "Icon_Remove=/org/esa/snap/resources/images/icons/Remove16.gif"

})
public class ToolAdaptersManagementDialog extends ModalDialog {

    private AppContext appContext;
    private JTable operatorsTable = null;

    public ToolAdaptersManagementDialog(AppContext appContext, String title, String helpID) {
        super(appContext.getApplicationWindow(), title, ID_CLOSE, helpID);
        this.appContext = appContext;

        //compute content and other buttons
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        JPanel buttonsPanel = createButtonsPanel();
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(createPropertiesPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JScrollPane(createAdaptersPanel()));
        setContent(panel);
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        AbstractButton newButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_New()), false);
        newButton.setToolTipText(Bundle.ToolTipNewOperator_Text());
        newButton.addActionListener(e -> {
            close();
            ToolAdapterOperatorDescriptor newOperatorSpi = new ToolAdapterOperatorDescriptor(ToolAdapterConstants.OPERATOR_NAMESPACE + "DefaultOperatorName", ToolAdapterOp.class, "DefaultOperatorName", null, null, null, null, null);
            ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, getHelpID(), newOperatorSpi, true);
            dialog.show();
        });
        panel.add(newButton);

        AbstractButton copyButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Copy()),
                false);
        copyButton.setToolTipText(Bundle.ToolTipCopyOperator_Text());
        copyButton.addActionListener(e -> {
            close();

            ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            String opName = operatorDesc.getName();
            int newNameIndex = 0;
            while (GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(opName) != null) {
                newNameIndex++;
                opName = operatorDesc.getName() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + newNameIndex;
            }
            String opAlias = operatorDesc.getAlias() + ToolAdapterConstants.OPERATOR_GENERATED_NAME_SEPARATOR + newNameIndex;
            ToolAdapterOperatorDescriptor duplicatedOperatorSpi = new ToolAdapterOperatorDescriptor(operatorDesc, opName, opAlias);
            ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, getHelpID(), duplicatedOperatorSpi, newNameIndex);
            dialog.show();
        });
        panel.add(copyButton);

        AbstractButton editButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Edit()), false);
        editButton.setToolTipText(Bundle.ToolTipEditOperator_Text());
        editButton.addActionListener(e -> {
            close();
            ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, getHelpID(), operatorDesc, false);
            dialog.show();
        });
        panel.add(editButton);

        AbstractButton runButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Execute()), false);
        runButton.setToolTipText(Bundle.ToolTipExecuteOperator_Text());
        runButton.addActionListener(e -> {
            close();
            ToolAdapterOperatorDescriptor operatorSpi = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            final ToolAdapterExecutionDialog operatorDialog = new ToolAdapterExecutionDialog(
                    operatorSpi,
                    appContext,
                    operatorSpi.getLabel(),
                    getHelpID());
            operatorDialog.show();
        });
        panel.add(runButton);

        AbstractButton delButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(Bundle.Icon_Remove()), false);
        delButton.setToolTipText(Bundle.ToolTipDeleteOperator_Text());
        delButton.addActionListener(e -> {
            close();
            ToolAdapterOperatorDescriptor descriptor = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
            ToolAdapterActionRegistrar.removeOperatorMenu(descriptor);
            ToolAdapterIO.removeOperator(descriptor);
        });
        panel.add(delButton);

        return panel;
    }

    private JTable createPropertiesPanel() {
        DefaultTableModel model = new DefaultTableModel(1, 2) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };
        model.setValueAt("User-defined adapter path", 0, 0);
        model.setValueAt(ToolAdapterIO.getUserAdapterPath(), 0, 1);
        model.addTableModelListener(l -> {
            String newPath = model.getValueAt(0, 1).toString();
            File path = new File(newPath);
            if (!path.exists() &&
                    SnapDialogs.Answer.YES == SnapDialogs.requestDecision("Path does not exist", "The path you have entered does not exist.\nDo you want to create it?", true, "Don't ask me in the future")) {
                if (!path.mkdirs()) {
                    SnapDialogs.showError("Path could not be created!");
                }
            }
            if (path.exists()) {
                try {
                    Preferences modulePrefs = NbPreferences.forModule(ToolAdapterIO.class);
                    modulePrefs.put("user.module.path", newPath);
                    modulePrefs.sync();
                    SnapDialogs.showInformation("The path for user adapters will be considered next time the applicaiton is opened.", "Don't show this dialog");
                } catch (BackingStoreException e1) {
                    SnapDialogs.showError(e1.getMessage());
                }
            }
        });
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(250);
        table.getColumnModel().getColumn(1).setMaxWidth(570);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setRowHeight(20);
        table.setBorder(BorderFactory.createLineBorder(Color.black));
        table.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                Object source = e.getSource();
                if (!table.equals(source)) {
                    table.editingCanceled(new ChangeEvent(source));
                    table.clearSelection();
                }
            }
        });
        return table;
    }

    private JTable createAdaptersPanel() {
        java.util.List<ToolAdapterOperatorDescriptor> toolboxSpis = new ArrayList<>();
        toolboxSpis.addAll(ToolAdapterRegistry.INSTANCE.getOperatorMap().values()
                                .stream()
                                .map(e -> (ToolAdapterOperatorDescriptor)e.getOperatorDescriptor())
                                .collect(Collectors.toList()));
        toolboxSpis.sort((o1, o2) -> o1.getAlias().compareTo(o2.getAlias()));
        OperatorsTableModel model = new OperatorsTableModel(toolboxSpis);
        operatorsTable = new JTable(model);
        operatorsTable.getColumnModel().getColumn(0).setMaxWidth(20);
        operatorsTable.getColumnModel().getColumn(1).setMaxWidth(250);
        operatorsTable.getColumnModel().getColumn(2).setMaxWidth(500);
        operatorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        operatorsTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int selectedRow = operatorsTable.getSelectedRow();
                    operatorsTable.getModel().setValueAt(true, selectedRow, 0);
                    close();
                    ToolAdapterOperatorDescriptor operatorDesc = ((OperatorsTableModel) operatorsTable.getModel()).getFirstCheckedOperator();
                    ToolAdapterEditorDialog dialog = new ToolAdapterEditorDialog(appContext, getHelpID(), operatorDesc, false);
                    dialog.show();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        return operatorsTable;
    }


}
