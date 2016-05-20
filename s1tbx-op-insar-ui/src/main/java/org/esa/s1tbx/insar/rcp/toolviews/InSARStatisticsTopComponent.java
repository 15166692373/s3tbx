/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.toolviews;

import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.InSARStatistic;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatBaselines;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatESDHistogram;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatESDMeasure;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatInSARInfo;
import org.esa.s1tbx.insar.rcp.toolviews.insar_statistics.StatResiduals;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.AUTO;

/**
 * Displays InSAR Statistics
 */
@TopComponent.Description(
        preferredID = "InSARStatisticsTopComponent",
        iconBase = "org/esa/s1tbx/insar/icons/stack24.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false, position = 200)
@ActionID(category = "Window", id = "org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows/Radar"),
        @ActionReference(path = "Toolbars/Radar Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_InSARStatisticsTopComponentName",
        preferredID = "InSARStatisticsTopComponent"
)
@NbBundle.Messages({
        "CTL_InSARStatisticsTopComponentName=InSAR Stack",
        "CTL_InSARStatisticsTopComponentDescription=InSAR related information on a stack",
})
public class InSARStatisticsTopComponent extends TopComponent {

    public static final String EmptyMsg = "This tool window requires a coregistered stack product to be selected";

    private final List<InSARStatistic> statisticList = new ArrayList<>();
    private ProductNode oldNode = null;

    public InSARStatisticsTopComponent() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.CTL_InSARStatisticsTopComponentName());
        setToolTipText(Bundle.CTL_InSARStatisticsTopComponentDescription());
        add(createPanel(), BorderLayout.CENTER);

        final SnapApp snapApp = SnapApp.getDefault();
        snapApp.getProductManager().addListener(new ProductManagerListener());
        snapApp.getSelectionSupport(ProductNode.class).addHandler(new SelectionSupport.Handler<ProductNode>() {
            @Override
            public void selectionChange(@NullAllowed ProductNode oldValue, @NullAllowed ProductNode newValue) {
                if (newValue != null && newValue != oldNode) {
                    final Product product = newValue.getProduct();
                    for (InSARStatistic statistic : statisticList) {
                        statistic.update(product);
                    }
                    oldNode = newValue;
                }
            }
        });

        Product product = snapApp.getSelectedProduct(AUTO);
        if(InSARStatistic.isValidProduct(product)) {
            for (InSARStatistic statistic : statisticList) {
                statistic.update(product);
            }
        }
    }

    public JPanel createPanel() {

        statisticList.add(new StatInSARInfo());
        statisticList.add(new StatResiduals());
        statisticList.add(new StatESDMeasure());
        statisticList.add(new StatESDHistogram());
        statisticList.add(new StatBaselines());

        final JTabbedPane tabbedPane = new JTabbedPane();
        for (InSARStatistic statistic : statisticList) {
            tabbedPane.add(statistic.getName(), statistic.createPanel());
        }
        tabbedPane.setSelectedIndex(0);

        final JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        return mainPanel;
    }

    public class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();
            for (InSARStatistic statistic : statisticList) {
                statistic.update(product);
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            for (InSARStatistic statistic : statisticList) {
                statistic.update(null);
            }
        }
    }
}
