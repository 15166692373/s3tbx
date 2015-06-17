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
package org.esa.s1tbx.io.polsarpro;

import org.esa.snap.dataio.envi.EnviProductReader;
import org.esa.snap.dataio.envi.Header;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PolsarProProductReader extends EnviProductReader {

    PolsarProProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = ReaderUtils.getFileFromInput(getInput());
        final File folder = inputFile.getParentFile();
        final File[] fileList = folder.listFiles();

        if(fileList == null) {
            throw new IOException("no files found in "+folder.toString());
        }

        final List<Header> headerList = new ArrayList<>(fileList.length);
        final HashMap<Header, File> headerFileMap = new HashMap<>(fileList.length);
        Header mainHeader = null;
        File mainHeaderFile = null;

        ResourceUtils.sortFileList(fileList);

        for (File file : fileList) {
            if (file.isDirectory())
                continue;
            if (file.getName().toLowerCase().endsWith("hdr")) {
                final File imgFile = getEnviImageFile(file);
                if (!imgFile.exists())
                    continue;

                final BufferedReader headerReader = getHeaderReader(file);
                try {

                    synchronized (headerReader) {
                        final Header header = new Header(headerReader);
                        headerList.add(header);
                        headerFileMap.put(header, file);

                        if (header.getNumBands() > 0 && header.getBandNames() != null) {
                            mainHeader = header;
                            mainHeaderFile = file;
                        }
                    }

                } finally {
                    if (headerReader != null) {
                        headerReader.close();
                    }
                }
            }
        }

        if (mainHeader == null)
            throw new IOException("Unable to read files");

        String productName;
        if (inputFile.isDirectory()) {
            productName = inputFile.getName();
            if (productName.equalsIgnoreCase("T3") || productName.equalsIgnoreCase("C3") ||
                    productName.equalsIgnoreCase("T4") || productName.equalsIgnoreCase("C4")) {
                productName = inputFile.getParentFile().getName() + "_" + productName;
            }
        } else {
            final String headerFileName = mainHeaderFile.getName();
            productName = headerFileName.substring(0, headerFileName.indexOf('.'));
        }

        final Product product = new Product(productName, mainHeader.getSensorType(),
                mainHeader.getNumSamples(), mainHeader.getNumLines());
        product.setProductReader(this);
        product.setDescription(mainHeader.getDescription());

        initGeoCoding(product, mainHeader);

        for (Header header : headerList) {
            initBands(headerFileMap.get(header), product, header);
        }

        applyBeamProperties(product, mainHeader.getBeamProperties());

        product.setFileLocation(mainHeaderFile);

        addMetadata(product, inputFile);

        return product;
    }

    private void addMetadata(final Product product, final File inputFile) throws IOException {
        if (!AbstractMetadata.hasAbstractedMetadata(product)) {
            final MetadataElement root = product.getMetadataRoot();
            final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, product.getName());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, product.getProductType());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, product.getSceneRasterWidth());
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, product.getSceneRasterHeight());

            AbstractMetadataIO.loadExternalMetadata(product, absRoot, inputFile);
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        // polsarpro data automatically calibrated for Radarsat2 only
        //absRoot.setAttributeInt(AbstractMetadata.abs_calibration_flag, 1);
    }
}
