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
package org.esa.nest.dat.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;
import org.esa.beam.framework.gpf.internal.RasterDataNodeValues;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.graphbuilder.GraphExecuter;
import org.esa.nest.gpf.ProgressMonitorList;
import org.esa.nest.gpf.ui.OperatorUI;
import org.esa.nest.gpf.ui.UIValidation;
import org.esa.nest.util.ResourceUtils;

import javax.media.jai.JAI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 */
public class NestSingleTargetProductDialog extends DefaultSingleTargetProductDialog {

    private final OperatorUI opUI;
    private JLabel statusLabel;
    private JComponent parametersPanel;

    public NestSingleTargetProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(operatorName, appContext, title, helpID);

        opUI = GraphExecuter.CreateOperatorUI(operatorName);

        addParameters();

        getJDialog().setMinimumSize(new Dimension(450, 450));
        getJDialog().setIconImage(ResourceUtils.esaPlanetIcon.getImage());

        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(255, 0, 0));
        this.getJDialog().getContentPane().add(statusLabel, BorderLayout.NORTH);
    }

    private void addParameters() {
        final PropertySet propertySet = parameterSupport.getPropertySet();
        final List<SourceProductSelector> sourceProductSelectorList = ioParametersPanel.getSourceProductSelectorList();

        if (sourceProductSelectorList.isEmpty()) {
            VisatApp.getApp().showErrorDialog("SourceProduct @Parameter not found in operator");
        } else {

            sourceProductSelectorList.get(0).addSelectionChangeListener(new AbstractSelectionChangeListener() {

                @Override
                public void selectionChanged(SelectionChangeEvent event) {
                    final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                    if (selectedProduct != null) { //&& form != null) {
                        final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
                        targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
                        opUI.setSourceProducts(new Product[]{selectedProduct});
                    }
                }
            });
        }

        if (propertySet.getProperties().length > 0) {
            if (!sourceProductSelectorList.isEmpty()) {
                Property[] properties = propertySet.getProperties();
                List<PropertyDescriptor> rdnTypeProperties = new ArrayList<PropertyDescriptor>(properties.length);
                for (Property property : properties) {
                    PropertyDescriptor parameterDescriptor = property.getDescriptor();
                    if (parameterDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                        rdnTypeProperties.add(parameterDescriptor);
                    }
                }
                rasterDataNodeTypeProperties = rdnTypeProperties.toArray(
                        new PropertyDescriptor[rdnTypeProperties.size()]);
            }
        }
    }

    protected void initForm() {
        form = new JTabbedPane();
        form.add("I/O Parameters", ioParametersPanel);

        parametersPanel = opUI.CreateOpTab(operatorName, parameterSupport.getParameterMap(), appContext);
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        form.add("Processing Parameters", new JScrollPane(parametersPanel));
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        if (validateUI()) {
            opUI.updateParameters();

            final HashMap<String, Product> sourceProducts = ioParametersPanel.createSourceProductsMap();
            return GPF.createProduct(operatorName, parameterSupport.getParameterMap(), sourceProducts);
        }
        return null;
    }

    private boolean validateUI() {
        final UIValidation validation = opUI.validateParameters();
        if (validation.getState() == UIValidation.State.WARNING) {
            final String msg = "Warning: " + validation.getMsg() +
                    "\n\nWould you like to continue?";
            return VisatApp.getApp().showQuestionDialog(msg, null) == 0;
        } else if (validation.getState() == UIValidation.State.ERROR) {
            final String msg = "Error: " + validation.getMsg();
            VisatApp.getApp().showErrorDialog(msg);
            return false;
        }
        return true;
    }

    @Override
    protected void onApply() {
        if (!canApply()) {
            return;
        }

        String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        appContext.getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);

        Product targetProduct = null;
        try {
            targetProduct = createTargetProduct();
            if (targetProduct == null) {
                //throw new NullPointerException("Target product is null.");
            }
        } catch (Throwable t) {
            handleInitialisationError(t);
        }
        if (targetProduct == null) {
            return;
        }

        targetProduct.setName(targetProductSelector.getModel().getProductName());
        if (targetProductSelector.getModel().isSaveToFileSelected()) {
            targetProduct.setFileLocation(targetProductSelector.getModel().getProductFile());
            final ProgressMonitorSwingWorker worker = new ProductWriterWorker(targetProduct);
            //worker.executeWithBlocking();
            worker.execute();
        } else if (targetProductSelector.getModel().isOpenInAppSelected()) {
            appContext.getProductManager().addProduct(targetProduct);
            showOpenInAppInfo();
        }
    }


    private class ProductWriterWorker extends ProgressMonitorSwingWorker<Product, Object> {

        private final Product targetProduct;
        private long saveTime;
        private Date executeStartTime;

        private ProductWriterWorker(Product targetProduct) {
            super(getJDialog(), "Writing Target Product");
            this.targetProduct = targetProduct;
        }

        @Override
        protected Product doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            pm.beginTask("Writing...", model.isOpenInAppSelected() ? 100 : 95);
            ProgressMonitorList.instance().add(pm);       //NESTMOD
            saveTime = 0L;
            Product product = null;
            try {
                // free cache	// NESTMOD
                JAI.getDefaultInstance().getTileCache().flush();
                System.gc();

                executeStartTime = Calendar.getInstance().getTime();
                long t0 = System.currentTimeMillis();
                Operator operator = null;
                if (targetProduct.getProductReader() instanceof OperatorProductReader) {
                    final OperatorProductReader opReader = (OperatorProductReader) targetProduct.getProductReader();
                    if (opReader.getOperatorContext().getOperator() instanceof Output) {
                        operator = opReader.getOperatorContext().getOperator();
                    }
                }
                if (operator == null) {
                    WriteOp writeOp = new WriteOp(targetProduct, model.getProductFile(), model.getFormatName());
                    writeOp.setDeleteOutputOnFailure(true);
                    writeOp.setWriteEntireTileRows(true);
                    writeOp.setClearCacheAfterRowWrite(false);
                    operator = writeOp;
                }
                final OperatorExecutor executor = OperatorExecutor.create(operator);
                executor.execute(SubProgressMonitor.create(pm, 95));

                saveTime = System.currentTimeMillis() - t0;
                File targetFile = model.getProductFile();
                if (model.isOpenInAppSelected() && targetFile.exists()) {
                    product = ProductIO.readProduct(targetFile);
                    if (product == null) {
                        product = targetProduct; // todo - check - this cannot be ok!!! (nf)
                    }
                    pm.worked(5);
                }
            } finally {
                // free cache
                JAI.getDefaultInstance().getTileCache().flush();
                System.gc();

                pm.done();
                ProgressMonitorList.instance().remove(pm); //NESTMOD
                if (product != targetProduct) {
                    targetProduct.dispose();
                }
            }
            return product;
        }

        @Override
        protected void done() {
            final TargetProductSelectorModel model = getTargetProductSelector().getModel();
            try {
                final Date now = Calendar.getInstance().getTime();
                final long diff = (now.getTime() - executeStartTime.getTime()) / 1000;
                if (diff > 120) {
                    final float minutes = diff / 60f;
                    statusLabel.setText("Processing completed in " + minutes + " minutes");
                } else {
                    statusLabel.setText("Processing completed in " + diff + " seconds");
                }

                final Product targetProduct = get();
                if (model.isOpenInAppSelected()) {
                    appContext.getProductManager().addProduct(targetProduct);
                    //showSaveAndOpenInAppInfo(saveTime);
                } else {
                    //showSaveInfo(saveTime);
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                handleProcessingError(e.getCause());
            } catch (Throwable t) {
                handleProcessingError(t);
            }
        }
    }
}