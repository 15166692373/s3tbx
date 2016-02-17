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
package org.esa.s1tbx.insar.gpf.ui.coregistration;

import org.esa.s1tbx.insar.gpf.coregistration.WarpOp;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for WarpOp
 */
public class WarpOpUI extends BaseOperatorUI {

    private final JComboBox warpPolynomialOrder = new JComboBox(new String[]{"1", "2", "3"});
    private final JComboBox interpolationMethod = new JComboBox(new String[]{
            WarpOp.NEAREST_NEIGHBOR, WarpOp.BILINEAR, WarpOp.BICUBIC, WarpOp.BICUBIC2,
            WarpOp.TRI, WarpOp.CC4P, WarpOp.CC6P, WarpOp.TS6P, WarpOp.TS8P, WarpOp.TS16P});

    private final JComboBox<String> rmsThreshold = new JComboBox(new String[]{"0.001", "0.05", "0.1", "0.5", "1.0"});

    private final JCheckBox inSAROptimizedCheckBox = new JCheckBox("InSAR Optimized");
    private Boolean inSAROptimized;

    private final JCheckBox demRefinementCheckBox = new JCheckBox("Offset Refinement Based on DEM");
    private Boolean demRefinement;
    private final JComboBox<String> demName = new JComboBox<>(DEMFactory.getDEMNameList());

    private final JCheckBox openResidualsFileCheckBox = new JCheckBox("Show Residuals");
    private boolean openResidualsFile;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        inSAROptimizedCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                inSAROptimized = (e.getStateChange() == ItemEvent.SELECTED);
                enableDemFields();
            }
        });

        demRefinementCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                demRefinement = (e.getStateChange() == ItemEvent.SELECTED);
                enableDemFields();
            }
        });

        openResidualsFileCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                openResidualsFile = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        rmsThreshold.setSelectedItem(String.valueOf(paramMap.get("rmsThreshold")));
        warpPolynomialOrder.setSelectedItem(paramMap.get("warpPolynomialOrder"));

        if (sourceProducts != null && sourceProducts.length > 0) {
            final InputProductValidator validator = new InputProductValidator(sourceProducts[0]);
            if (!validator.isComplex()) {
                interpolationMethod.removeAllItems();
                interpolationMethod.addItem(WarpOp.NEAREST_NEIGHBOR);
                interpolationMethod.addItem(WarpOp.BILINEAR);
                interpolationMethod.addItem(WarpOp.BICUBIC);
                interpolationMethod.addItem(WarpOp.BICUBIC2);
            }
        }

        interpolationMethod.setSelectedItem(paramMap.get("interpolationMethod"));

        inSAROptimized = (Boolean) paramMap.get("inSAROptimized");
        if (inSAROptimized == null) {
            inSAROptimized = false;
        }
        inSAROptimizedCheckBox.setSelected(inSAROptimized);

        demRefinement = (Boolean) paramMap.get("demRefinement");
        if (demRefinement == null) {
            demRefinement = false;
        }
        demRefinementCheckBox.setSelected(demRefinement);

        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null) {
            ElevationModelDescriptor descriptor = ElevationModelRegistry.getInstance().getDescriptor(demNameParam);
            if (descriptor != null) {
                demName.setSelectedItem(DEMFactory.getDEMDisplayName(descriptor));
            } else {
                demName.setSelectedItem(demNameParam);
            }
        }
        enableDemFields();
    }


    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("rmsThreshold", Float.parseFloat((String)rmsThreshold.getSelectedItem()));
        paramMap.put("warpPolynomialOrder", Integer.parseInt((String) warpPolynomialOrder.getSelectedItem()));
        paramMap.put("interpolationMethod", interpolationMethod.getSelectedItem());

        paramMap.put("inSAROptimized", inSAROptimized);
        paramMap.put("demRefinement", demRefinement);
        if (demRefinement) {
            paramMap.put("demName", DEMFactory.getProperDEMName((String) demName.getSelectedItem()));
        }

        paramMap.put("openResidualsFile", openResidualsFile);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "RMS Threshold (pixel accuracy):", rmsThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Warp Polynomial Order:", warpPolynomialOrder);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Method:", interpolationMethod);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(inSAROptimizedCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(demRefinementCheckBox, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(openResidualsFileCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableDemFields() {
        demRefinementCheckBox.setEnabled(inSAROptimized);
        demName.setEnabled(demRefinement);
    }
}
