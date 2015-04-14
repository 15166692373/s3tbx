package org.jlinda.nest.dat.dialogs;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.dat.dialogs.ProductSetPanel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.db.CommonReaders;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.ModelessDialog;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.DialogUtils;
import org.esa.snap.util.ProductOpener;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.nest.stacks.MasterSelection;
import org.jlinda.nest.stacks.OptimalMaster;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Find Optimal Master product for InSAR
 */
public class InSARMasterDialog extends ModelessDialog {

    DecimalFormat df = new DecimalFormat("0.00");

    private boolean ok = false;

    private final InSARFileModel outputFileModel = new InSARFileModel();
    private final SnapApp.SnapContext appContext = new SnapApp.SnapContext();
    private final ProductSetPanel inputProductListPanel = new ProductSetPanel(appContext, "Input stack");
    private final ProductSetPanel outputProductListPanel = new ProductSetPanel(appContext, "Overview", outputFileModel);

    private final Map<SLCImage, File> slcFileMap = new HashMap<SLCImage, File>(10);

    private JButton openBtn;
    private final JCheckBox searchDBCheckBox = new JCheckBox("Search Product Library");

    public InSARMasterDialog() {
        super(SnapApp.getDefault().getMainFrame(), "Stack Overview and Optimal InSAR Master Selection", ModalDialog.ID_OK_CANCEL_HELP, "InSARMaster");

        getButton(ID_OK).setText("Overview");
        getButton(ID_CANCEL).setText("Close");

        initContent();
    }

    private void initContent() {
        final JPanel contentPane = new JPanel(new BorderLayout());

        final JPanel buttonPanel1 = new JPanel(new GridLayout(10, 1));
        final JButton addAllBtn = DialogUtils.createButton("addAllBtn", "Add Opened", null, buttonPanel1, DialogUtils.ButtonStyle.Text);
        addAllBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
                final List<File> fileList = new ArrayList<File>(products.length);
                for (Product prod : products) {
                    final File file = prod.getFileLocation();
                    if (file != null && file.exists()) {
                        fileList.add(file);
                    }
                }
                inputProductListPanel.setProductFileList(fileList.toArray(new File[fileList.size()]));
            }
        });
        buttonPanel1.add(addAllBtn);

        final JButton clearBtn = DialogUtils.createButton("clearBtn", "Clear", null, buttonPanel1, DialogUtils.ButtonStyle.Text);
        clearBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                inputProductListPanel.setProductFileList(new File[]{});
            }
        });
        buttonPanel1.add(clearBtn);

        inputProductListPanel.add(buttonPanel1, BorderLayout.EAST);
        contentPane.add(inputProductListPanel, BorderLayout.NORTH);

        // option pane
        final JPanel optionsPane = new JPanel(new GridBagLayout());
        optionsPane.setBorder(BorderFactory.createTitledBorder("Options"));
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.gridy++;
        DialogUtils.addComponent(optionsPane, gbc, "", searchDBCheckBox);
        gbc.gridy++;
        // not yet working
        //contentPane.add(optionsPane, BorderLayout.CENTER);

        final JPanel buttonPanel2 = new JPanel(new GridLayout(10, 1));
        openBtn = DialogUtils.createButton("openButton", "     Open     ", null, buttonPanel2, DialogUtils.ButtonStyle.Text);
        openBtn.setEnabled(false);
        openBtn.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                File[] files = outputProductListPanel.getSelectedFiles();
                if (files.length == 0)                      // default to get all files
                    files = outputProductListPanel.getFileList();
                final ProductOpener opener = new ProductOpener();
                opener.openProducts(files);
            }
        });
        buttonPanel2.add(openBtn);
        outputProductListPanel.add(buttonPanel2, BorderLayout.EAST);
        contentPane.add(outputProductListPanel, BorderLayout.SOUTH);

        setContent(contentPane);
    }

    public void setInputProductList(final ProductEntry[] productEntryList) {
        inputProductListPanel.setProductEntryList(productEntryList);
        processStack();
    }

    private void validate() throws Exception {
        final File[] inputFiles = inputProductListPanel.getFileList();
        if (inputFiles.length < 2) {
            throw new Exception("Please select at least two SLC products");
        }
    }

    private void processStack() {
        try {
            validate();

            final File[] inputFiles = inputProductListPanel.getFileList();

            final MasterSelection.IfgStack[] ifgStack = findInSARProducts(inputFiles);

            if (ifgStack == null) {
                openBtn.setEnabled(false);
                SnapDialogs.showWarning("Optimal master not found");
            } else {
                final OptimalMaster dataStack = new MasterSelection();
                final int masterIndex = dataStack.findOptimalMaster(ifgStack);
                final MasterSelection.IfgPair[] slaveList = ifgStack[masterIndex].getMasterSlave();

                updateData(slaveList, masterIndex);

                openBtn.setEnabled(true);
                ok = true;
            }
        } catch (Exception e) {
            SnapDialogs.showError("Error: " + e.getMessage());
        }
    }

    protected void onOK() {
        processStack();
    }

    public boolean IsOK() {
        return ok;
    }

    private void updateData(final MasterSelection.IfgPair[] slaveList, final int masterIndex) {
        outputFileModel.clear();
        final File mstFile = slcFileMap.get(slaveList[masterIndex].getMasterMetadata());
//        String test = df.format(slaveList[masterIndex].getCoherence());
        try {
            final Product productMst = CommonReaders.readProduct(mstFile);
            final MetadataElement absRootMst = AbstractMetadata.getAbstractedMetadata(productMst);
            final String[] mstValues = new String[]{
                    productMst.getName(),
                    "Master",
                    OperatorUtils.getAcquisitionDate(absRootMst),
                    String.valueOf(absRootMst.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                    String.valueOf(absRootMst.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                    String.valueOf(df.format(slaveList[masterIndex].getPerpendicularBaseline())),
                    String.valueOf(df.format(slaveList[masterIndex].getTemporalBaseline())),
                    String.valueOf(df.format(slaveList[masterIndex].getCoherence())),
                    String.valueOf(df.format(slaveList[masterIndex].getHeightAmb())),
                    String.valueOf(df.format(slaveList[masterIndex].getDopplerDifference()))
            };
            outputFileModel.addFile(mstFile, mstValues);
        } catch (Exception e) {
            SnapDialogs.showError("Unable to read " + mstFile.getName() + '\n' + e.getMessage());
        }

        for (MasterSelection.IfgPair slave : slaveList) {
            final File slvFile = slcFileMap.get(slave.getSlaveMetadata());
            if (!slvFile.equals(mstFile)) {
                try {
                    final Product product = CommonReaders.readProduct(slvFile);
                    final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

                    final String[] slvValues = new String[]{
                            product.getName(),
                            "Slave",
                            OperatorUtils.getAcquisitionDate(absRoot),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT, 0)),
                            String.valueOf(absRoot.getAttributeInt(AbstractMetadata.ABS_ORBIT, 0)),
                            String.valueOf(df.format(slave.getPerpendicularBaseline())),
                            String.valueOf(df.format(slave.getTemporalBaseline())),
                            String.valueOf(df.format(slave.getCoherence())),
                            String.valueOf(df.format(slave.getHeightAmb())),
                            String.valueOf(df.format(slave.getDopplerDifference()))
                    };
                    outputFileModel.addFile(slvFile, slvValues);
                } catch (Exception e) {
                    SnapDialogs.showError("Unable to read " + slvFile.getName() + '\n' + e.getMessage());
                }
            }
        }
    }

    private MasterSelection.IfgStack[] findInSARProducts(final File[] inputFiles) {
        final int size = inputFiles.length;
        final List<SLCImage> imgList = new ArrayList<SLCImage>(size);
        final List<Orbit> orbList = new ArrayList<Orbit>(size);

        for (File file : inputFiles) {
            try {
                final Product product = CommonReaders.readProduct(file);
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final SLCImage img = new SLCImage(absRoot);
                final Orbit orb = new Orbit(absRoot, 3);

                slcFileMap.put(img, file);

                imgList.add(img);
                orbList.add(orb);
            } catch (IOException e) {
                SnapDialogs.showError("Error: unable to read " + file.getPath() + '\n' + e.getMessage());
            } catch (Exception e) {
                SnapDialogs.showError("Error: " + file.getPath() + '\n' + e.getMessage());
            }
        }

        try {
            final OptimalMaster dataStack = new MasterSelection();
            dataStack.setInput(imgList.toArray(new SLCImage[size]), orbList.toArray(new Orbit[size]));

            final Worker worker = new Worker(SnapApp.getDefault().getMainFrame(), "Computing Optimal InSAR Master",
                    dataStack);
            worker.executeWithBlocking();

            return (MasterSelection.IfgStack[]) worker.get();

        } catch (Throwable t) {
            SnapDialogs.showError("Error:" + t.getMessage());
            return null;
        }
    }

    private static class Worker extends ProgressMonitorSwingWorker {
        private final OptimalMaster dataStack;

        Worker(final Component component, final String title,
               final OptimalMaster optimalMaster) {
            super(component, title);
            this.dataStack = optimalMaster;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            return dataStack.getCoherenceScores(pm);
        }
    }

}
