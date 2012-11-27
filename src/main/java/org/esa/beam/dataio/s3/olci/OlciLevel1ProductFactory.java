package org.esa.beam.dataio.s3.olci;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.dataio.s3.AbstractProductFactory;
import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class OlciLevel1ProductFactory extends AbstractProductFactory {

    private static final float[] spectralWavelengths = new float[21];
    private static final float[] spectralBandwidths = new float[21];

    static {
        getSpectralBandsProperties(spectralWavelengths, spectralBandwidths);
    }

    static void getSpectralBandsProperties(float[] wavelengths, float[] bandwidths) {
        final Properties properties = new Properties();

        try {
            properties.load(OlciLevel1ProductFactory.class.getResourceAsStream("spectralBands.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        for (int i = 0; i < wavelengths.length; i++) {
            wavelengths[i] = Float.parseFloat(properties.getProperty("wavelengths." + i));
        }
        for (int i = 0; i < bandwidths.length; i++) {
            bandwidths[i] = Float.parseFloat(properties.getProperty("bandwidths." + i));
        }
    }

    public OlciLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final File directory = getInputFileParentDirectory();

        final List<String> fileNameList = new ArrayList<String>();
        collectFileNames(directory, fileNameList, true);
        collectFileNames(directory, fileNameList, false);

        return Collections.unmodifiableList(fileNameList);
    }

    private static void collectFileNames(File directory, List<String> fileNameList, final boolean acceptRadiances) {
        final String[] radianceFileNames = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc") && (acceptRadiances == name.contains("radiances") && !name.contains("timeCoordinates"));
            }
        });
        fileNameList.addAll(Arrays.asList(radianceFileNames));
    }

    @Override
    protected ProductSubsetDef getSubsetDef(String fileName) {
        ProductSubsetDef subsetDef = null;
        if (fileName.equals("generalInfo.nc")) {
            subsetDef = new ProductSubsetDef();
            subsetDef.addNodeName("detector_index");
        }
        return subsetDef;
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("TOA_radiances_Oa:error_estimates_Oa:TOA_radiances_Ob:error_estimates_Ob");
    }

    @Override
    protected RasterDataNode addSpecialNode(Band sourceBand, Product targetProduct) {
        final Product sourceProduct = sourceBand.getProduct();
        final MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        final MetadataElement globalAttributes = metadataRoot.getElement("Global_Attributes");
        if (!globalAttributes.containsAttribute("subsampling_factor")) {
            final int subSampling = globalAttributes.getAttributeInt("subsampling_factor");

            return copyBandAsTiePointGrid(sourceBand, targetProduct, subSampling, subSampling, 0.0f, 0.0f);
        } else {
            // TODO - handle timeCoordinates.nc and removedPixels.nc
            return null;
        }
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        if (targetProduct.getTiePointGrid("TP_latitude") != null && targetProduct.getTiePointGrid("TP_longitude") != null) {
            targetProduct.setGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("TP_latitude"),
                                                             targetProduct.getTiePointGrid("TP_longitude")));
        }
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode instanceof Band) {
            final Band targetBand = (Band) targetNode;
            final String sourceBandName = sourceBand.getName();
            if (sourceBandName.matches("TOA_radiances_Oa[0-2][0-9]")) {
                final int channel = Integer.parseInt(sourceBandName.substring(16, 18));
                targetBand.setSpectralBandIndex(channel - 1);
                targetBand.setSpectralWavelength(spectralWavelengths[channel - 1]);
                targetBand.setSpectralBandwidth(spectralBandwidths[channel - 1]);
            }
        }
    }

}
