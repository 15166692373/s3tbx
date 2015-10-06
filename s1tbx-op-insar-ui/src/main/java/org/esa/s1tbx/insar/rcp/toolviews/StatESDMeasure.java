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

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.rcp.SnapApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;


public class StatESDMeasure implements InSARStatistic {

    private JPanel panel;
    private ChartPanel chartPanel;
    private JTextArea textarea;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private Map<String, Map<Integer, Double>> esdData = new HashMap<>();

    private static final String TITLE = "Estimated Shifts per Burst Overlap";
    private static final String XAXIS_LABEL = "Burst Overlap #";
    private static final String YAXIS_LABEL = "ESD Measurement [cm]";

    public static final String EmptyMsg = "This tool window requires a coregistered TOPSAR stack product to be selected with Azimuth Shift applied to it.";

    public String getName() {
        return "ESD Measure";
    }

    public Component createPanel() {

        // Add the series to your data set
        dataset = new XYSeriesCollection();

        // Generate the graph
        chart = ChartFactory.createXYLineChart(
                TITLE, // Title
                XAXIS_LABEL, // x-axis Label
                YAXIS_LABEL, // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
        );

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel = new JPanel(new BorderLayout());
        textarea = new JTextArea(EmptyMsg);
        panel.add(textarea, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        setVisible(false);

        return panel;
    }

    public void update(final Product product) {
        try {
            if (InSARStatisticsTopComponent.isValidProduct(product) && readESDMeasure(product)) {
                setVisible(true);

                dataset.removeAllSeries();

                int i = 0;
                for(String subswath : esdData.keySet()) {

                    final XYSeries series = new XYSeries(subswath);
                    final Map<Integer, Double> values = esdData.get(subswath);
                    for(Integer burst : values.keySet()) {
                        series.add(burst+1, values.get(burst));
                    }

                    dataset.addSeries(series);

                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)chart.getXYPlot().getRenderer();
                    renderer.setSeriesLinesVisible(i, true);
                    renderer.setSeriesShapesVisible(i, true);

                    chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                    ++i;
                }
            } else {
                setVisible(false);
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    private void setVisible(final boolean flag) {
        textarea.setVisible(!flag);
        chartPanel.setVisible(flag);
    }

    private synchronized boolean readESDMeasure(final Product product) {
        esdData.clear();

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if(absRoot != null) {
            final MetadataElement esdElem = absRoot.getElement("ESD_Measurement");
            if (esdElem != null) {
                final MetadataElement[] subSwathElems = esdElem.getElements();
                if (subSwathElems != null) {

                    for (MetadataElement subSwathElem : subSwathElems) {
                        final Map<Integer, Double> shiftMap = new HashMap<>(9);
                        esdData.put(subSwathElem.getName(), shiftMap);

                        final MetadataElement[] overlapElems = subSwathElem.getElements();
                        if (overlapElems != null) {
                            for (MetadataElement overlapElem : overlapElems) {
                                int overlapIndex = overlapElem.getAttributeInt("overlapIndex");
                                double azimuthShift = overlapElem.getAttributeDouble("azimuthShift");

                                shiftMap.put(overlapIndex, azimuthShift);
                            }
                        }
                    }
                }
            }
        }
        return !esdData.isEmpty();
    }
}

